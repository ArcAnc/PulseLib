# Render Types and Queue

Most mods can use PulseLib's renderers without touching the render queue directly. This page exists for the cases where you need to understand why PulseLib does not use vanilla `RenderType` values and where custom rendering should be inserted.

PulseLib models are triangle meshes loaded from glTF/GLB or another model loader. Vanilla baked block models are mostly quad-based, so PulseLib provides its own triangle render types, shaders, vertex format, and instanced queue. That is why examples use [`PRenderTypes`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java) instead of `RenderType.entityCutout` or block render types.

## Which render type should I choose?

Use the simplest type that matches the visual result:

* `trianglesSolid` for opaque models.
* `trianglesCutout` for hard alpha cutouts, like holes or masked pixels.
* `trianglesTranslucent` for glass-like transparency and weighted blended order-independent transparency (OIT).

Use those queued variants in `PBlockRenderer`, `PEntityRenderer`, and `PItemRenderer`, including GUI item rendering. `trianglesGui` is a compatibility alias for the translucent instant pipeline used by direct `PBakedBone.instantDraw(...)` calls; it is not a queued item-renderer type.

`trianglesSolid` forces an opaque output alpha. Cutout and translucent variants discard fragments below their alpha threshold. Built-in queued translucent variants use weighted blended order-independent transparency when the target has a depth attachment and the OpenGL driver supports independent per-target blending. The OIT pass keeps several depth layers, so overlapping transparent PulseLib meshes are resolved correctly without relying on submission order. If that path is unavailable, PulseLib falls back to the queue's back-to-front alpha blending.

For emissive meshes the built-in renderers select the matching solid, cutout, or translucent emissive variant automatically from the base type.

In a renderer constructor this usually looks like:

```java
super(modelData, PRenderTypes.RenderTypeProvider::trianglesSolid);
```

If a texture is marked as emissive, the default renderers automatically switch the mesh to the matching emissive variant. You normally do not need to select an emissive type yourself.

Alpha-mode and emissive texture metadata is described on [Textures and Emissive](textures-and-emissive.md).

## Why vanilla RenderType is not enough

PulseLib's baked meshes use [`PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java). The queued triangle pipeline receives `DynamicTransforms` and `Lighting` through the normal 26.1 uniforms, but per-instance transform, colour, light, overlay, and deformer offsets arrive as instanced vertex attributes. The instant pipeline instead receives one mesh's colour, light, overlay, and deformer offsets through `ColorOverlay`.

A vanilla render type may compile and still render incorrectly because its shader and vertex format do not match this contract.

If you create a custom render type, keep these requirements:

* `VertexFormat.Mode.TRIANGLES`
* `PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL`
* a shader that understands PulseLib's uniforms and instance attributes
* a transparency state that matches how the queue should sort the mesh

For most mods, it is safer to start from PulseLib's existing render types and only add a new one when you need a genuinely different shader state.

## What the queue does

[`PRenderQueue`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java) batches identical meshes together and renders many instances with one instanced draw call. That is important for animated block entities and entities: every object can have its own transform and animation pose, but the GPU can still draw repeated mesh buffers efficiently.

The queue has a few stages:

* `SOLID_BLOCKS` for solid block entity meshes.
* `TRANSLUCENT_BLOCKS` for transparent block/entity-adjacent meshes.
* `ENTITIES` for entity and hand-held item rendering.
* `FIRST_PERSON` for items rendered in either first-person hand.
* `GUI` for GUI rendering.

Normal renderers submit into these stages for you. [`PRenderStagesHandler`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java) flushes the level stages, while PulseLib flushes `FIRST_PERSON` immediately after the hand pass. When an enabled player first-person animation is active, that pass is PulseLib's replacement renderer rather than Minecraft's vanilla hands-and-items renderer. `PItemRenderer` selects `FIRST_PERSON` automatically for `FIRST_PERSON_LEFT_HAND` and `FIRST_PERSON_RIGHT_HAND`; no special renderer code is needed.

## When to submit manually

Manual queue submission is an advanced escape hatch. Use it when you already have a baked PulseLib mesh and want to draw it in the same pipeline.

```java
PRenderQueue.submit(
        PRenderQueue.RenderStage.ENTITIES,
        renderType,
        bakedMesh,
        null, // no mesh deformation
        new PRenderQueue.InstanceData(matrix, 0xFFFFFFFF, packedLight, packedOverlay));
```

`PBakedMesh` owns the GPU vertex and index buffers. Do not pass a vanilla `VertexBuffer`: the queue needs PulseLib's mesh data and its triangle vertex format.

For opaque submissions, identical `(RenderType, PBakedMesh)` pairs are grouped. Transparent submissions that cannot use OIT are ordered back-to-front and only adjacent equal pairs are joined. Built-in translucent types are accumulated into OIT targets per Minecraft output target, then composited after `ENTITIES` and `TRANSLUCENT_BLOCKS` are flushed together from `RenderLevelStageEvent.AfterTranslucentFeatures`. First-person OIT is instead composited immediately after the hand pass. Each OIT target records transparent depth layers, allowing Minecraft's transparency post-chain to place the result correctly relative to translucent blocks. This makes supported PulseLib translucency independent of its own submission order; custom transparent types, targets without depth, and unsupported OpenGL contexts retain sorted alpha blending. Static mesh data is packed into OpenGL geometry pages; the queue streams per-instance records and indirect commands through fence-protected ring buffers, using multi-draw indirect where the current OpenGL driver supports it. These are implementation details: callers only submit meshes and must not retain or manipulate the queue's buffers.

## Renderer hooks

Every PulseLib renderer has the same three-stage flow:

* `preSubmit(PoseStack, renderState, CameraRenderState, SubmitNodeCollector)` runs before the model is submitted.
* `trueSubmit(...)` is the normal PulseLib model submission.
* `postSubmit(...)` runs after the model is submitted.

A typical customization is small:

```java
@Override
public void postSubmit(PoseStack poseStack,
                       RobotRenderState renderState,
                       CameraRenderState cameraRenderState,
                       SubmitNodeCollector submitNodeCollector) {
    // Submit additional PulseLib meshes or collector nodes here.
}
```

Classes used:

* [`PRenderTypes`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java)
* [`PRenderQueue`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java)
* [`PRenderStagesHandler`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java)
