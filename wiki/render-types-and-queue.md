# Render Types and Queue

Most mods can use PulseLib's renderers without touching the render queue directly. This page explains why PulseLib does not use vanilla `RenderType` values and how to add custom geometry to the current render backend. For its internal frame-plan, geometry-arena, and capability paths, see [Render backend](render-backend.md).

PulseLib models are triangle meshes loaded from glTF/GLB or another model loader. Vanilla baked block models are mostly quad-based, so PulseLib provides its own triangle render types, shaders, vertex format, and instanced queue. That is why examples use [`PRenderTypes`](../common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java) instead of `RenderType.entityCutout` or block render types.

## Which render type should I choose?

Use the simplest type that matches the visual result:

* `trianglesSolid` for opaque models.
* `trianglesCutout` for hard alpha cutouts, like holes or masked pixels.
* `trianglesTranslucent` for glass-like transparency and weighted OIT.
* `trianglesGui` for direct GUI drawing.
* `trianglesImmediate` for advanced direct drawing outside the queued world path.

The first three types use the queued instanced shaders. `trianglesGui` and `trianglesImmediate` share the direct `triangles_immediate_lit` shader and differ in transparency state. There is no `trianglesLit` compatibility method in the current API. The full program mapping and shader data contract are documented on [Shaders](shaders.md).

In a renderer constructor this usually looks like:

```java
super(modelData, PRenderTypes.RenderTypeProvider::trianglesSolid);
```

If a texture is marked as emissive, the default renderers automatically switch the mesh to the matching emissive variant. You normally do not need to choose `trianglesSolidEmissive` yourself unless you are writing custom draw code.

Emissive and alpha-mode texture metadata is described on [Resources](resources.md).

## Why vanilla RenderType is not enough

PulseLib's baked meshes use [`PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL`](../common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java). Queued shaders also expect per-instance data: transform rows, color, light, overlay, and deformer stream offsets. A vanilla render type may compile and still render incorrectly because its shader and vertex format do not match the data PulseLib sends.

If you create a custom render type, keep these requirements:

* `VertexFormat.Mode.TRIANGLES`
* `PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL`
* a shader that implements PulseLib's queued attribute and sampler contract
* a transparency state that matches how the queue should sort the mesh

For most mods, it is safer to start from PulseLib's existing render types and only add a new one when you need a genuinely different shader state.

## What the queue does

[`PRenderQueue`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java) compiles submissions into a frame plan. Opaque submissions with the same render type and geometry are batched as instances. Transparent submissions use back-to-front ordering unless they use a built-in OIT render type; OIT submissions are batched by compatible render type and geometry, then resolved through four depth-peeled transparent layers. The OpenGL driver uploads the instance stream once per flush and uses multi-draw indirect where the current context supports it. It falls back to direct instanced draws when that capability is unavailable.

Static [`PGeometryData`](../common/src/main/java/com/arcanc/pulselib/content/renderer/plan/PGeometryData.java) is placed in the backend's geometry arena and reused across frames. This is the normal path for baked meshes. Dynamic geometry is reserved for CPU-deformed meshes and is more expensive because its vertex data is uploaded again.

The queue has a few stages:

* `SOLID_BLOCKS` for solid block entity meshes.
* `TRANSLUCENT_BLOCKS` for transparent block/entity-adjacent meshes.
* `ENTITIES` for entity and third-person item rendering.
* `FIRST_PERSON` for first-person item rendering.
* `GUI` for local GUI submissions; direct GUI model/item drawing bypasses the queue.

Normal renderers submit into these stages for you. [`PRenderStagesHandler`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java) flushes opaque `ENTITIES` and `SOLID_BLOCKS` after block entities, then flushes remaining `ENTITIES`, `SOLID_BLOCKS`, and `TRANSLUCENT_BLOCKS` together after particles. `GameRendererMixin` flushes `FIRST_PERSON` directly after the hand pass. With an enabled player first-person animation, that pass uses PulseLib's presentation for the affected arms and items.

World OIT is resolved after weather for Fast/Fancy graphics. Fabulous defers composition until after Minecraft's transparency chain and combines PulseLib layers with the native translucent, item/entity, particles, weather, and clouds targets. GUI/offscreen entity previews use separate scoped queues with sorted transparency; they do not contribute to world OIT. See [GUI rendering](gui-rendering.md#entity-previews-and-offscreen-targets).

## When to submit manually

Manual queue submission is an advanced escape hatch. Use it for static triangle data that you have packed in PulseLib's compatible vertex format. Do not submit a vanilla `VertexBuffer`: the queue now accepts `PGeometryData` for static meshes, plus a `PInstanceHeader` for each instance.

```java
PGeometryData geometry = createCompatibleTriangleGeometry();
PInstanceHeader instance = new PInstanceHeader(
		poseStack.last().pose(),
		0xFFFFFFFF,
		packedLight,
		packedOverlay);

PRenderQueue.submitEntityMesh(
		renderType,
		geometry,
		instance);
```

`PGeometryData` copies its supplied vertex and index buffers, so it is safe to retain and submit it repeatedly. Its vertex stride and index type must match the data you supplied. For the standard PulseLib shaders, use `VertexFormat.Mode.TRIANGLES` and `PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL` when packing the data. For most integrations, using a baked mesh's `geometry()` is preferable to hand-packing a buffer.

Use the stage-specific methods whenever possible:

* `submitBlockEntityMesh(...)` for opaque block-entity geometry.
* `submitBlockEntityTranslucentMesh(...)` for block-entity geometry that must be sorted as transparent.
* `submitEntityMesh(...)` for entities and held items.
* `submitItem(...)` when the item display context decides the stage.
* `submitFirstPersonMesh(...)` for geometry explicitly owned by the hand pass.

`submitItem` routes first-person hands to `FIRST_PERSON`, third-person hands and `HEAD` to `ENTITIES`, and `GROUND`, `FIXED`, and `NONE` to `TRANSLUCENT_BLOCKS`. It does not queue `GUI`: `PItemRenderer` draws that context immediately. `submitEntityMesh` follows an active first-person scope and otherwise uses `ENTITIES`; a GUI/offscreen scope captures those submissions locally.

The queue is flushed by `PRenderStagesHandler`. Do not call `flush(...)` from an ordinary renderer: flushing early breaks batching and can render at the wrong stage. A custom `RenderStage` is available only when you also own the matching flush point.

If you only need to draw a vanilla item, text, a beam, or a simple effect, it is often better to use the `MultiBufferSource` passed to `preSubmit` or `postSubmit`. That keeps vanilla rendering in vanilla's pipeline and PulseLib mesh rendering in PulseLib's pipeline.

## Renderer hooks

Every PulseLib renderer has the same three-stage flow:

* `preSubmit` runs before the model is submitted.
* `trueSubmit` is the normal PulseLib model submission.
* `postSubmit` runs after the model is submitted.

A typical customization is small:

```java
@Override
public void postSubmit(PoseStack poseStack,
		RobotEntity entity,
		Function<ResourceLocation, RenderType> renderType,
		MultiBufferSource bufferSource,
		int packedLight,
		int packedOverlay,
		float partialTick,
		Object... additionalData)
{
	// Add vanilla buffer rendering here, or submit extra PulseLib-compatible meshes.
}
```

Classes used:

* [`PRenderTypes`](../common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java)
* [`PRenderQueue`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java)
* [`PRenderStagesHandler`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java)
