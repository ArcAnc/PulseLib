# Render Types and Queue

PulseLib models are triangle meshes. [`PRenderTypes`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java) supplies their native render pipelines and material states; [`PRenderQueue`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java) compiles captured submissions and executes them through the shared GPU backend.

## Choosing a render type

| Material | Queued renderer | Direct `PBakedBone.instantDraw(...)` / GUI helper |
| --- | --- | --- |
| Opaque | `trianglesSolid` | `trianglesInstantSolid` |
| Hard alpha cutout | `trianglesCutout` | `trianglesInstantCutout` |
| Blended alpha | `trianglesTranslucent` | `trianglesInstantTranslucent` / `trianglesGui` |
| Emissive | `trianglesEmissiveSolid`, `trianglesEmissiveCutout`, `trianglesEmissiveTranslucent` | matching `trianglesInstantEmissive...` variant |

All factories above belong to `PRenderTypes.RenderTypeProvider` and take the atlas `Identifier`. `PBlockRenderer`, `PEntityRenderer`, and `PItemRenderer` use queued variants, including GUI items:

```java
super(modelData, PRenderTypes.RenderTypeProvider::trianglesSolid);
```

Solid forces output alpha to one. Cutout discards low-alpha fragments; translucent blends surviving fragments. Default mesh resolvers choose a matching emissive variant for emissive sprites. [Resources](resources.md#alpha-modes) explains alpha metadata and explicit per-mesh overrides.

Canonical factory aliases are also retained: `trianglesImmediate` uses instant cutout; `trianglesSolidEmissive`, `trianglesCutoutEmissive`, and `trianglesTranslucentEmissive` select queued emissive variants; `trianglesGuiEmissive` and `trianglesImmediateEmissive` select instant emissive translucent and cutout respectively. `emissiveVariant(...)` preserves the layout/alpha mode of known types and leaves an unknown custom type unchanged.

## Material transparency and OIT

A blended material and an OIT submission are separate choices. `PTransparencyMode.ORDINARY` sorts transparent instances back-to-front and alpha-blends them. `PTransparencyMode.OIT` requests PulseLib's order-independent path for a supported built-in queued translucent type. Explicit queue overloads accept that policy without changing the native `RenderType` identity.

Overloads without a policy default to OIT for the two supported queued translucent pipelines and to ordinary rendering for other types. Living attachments and player effects may explicitly use ordinary alpha even with a translucent material. Custom transparent types are not automatically OIT-enabled; explicitly requesting OIT for an unsupported type is rejected.

OIT also needs a suitable depth target and driver support. Unsupported execution uses ordinary sorted alpha. The current implementation keeps four depth layers and composites them into the appropriate native targets. The native `Improved Transparency` option changes the target layout and composition route; it does not change which material a renderer requests.

## Pipeline contract

Minecraft's native type is `net.minecraft.client.renderer.rendertype.RenderType`. A compatible custom type needs:

* `VertexFormat.Mode.TRIANGLES` and `PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL`.
* A shader that reads the queued instance attributes or the instant per-draw inputs appropriate to the caller.
* A native `RenderSetup` / `RenderPipeline` with the intended blend, depth and target state.

Vanilla block/entity pipelines usually use different mesh layouts and cannot consume PulseLib instances correctly. `RenderPipeline.getColorTargetState().blendFunction()` exposes the native `Optional<BlendFunction>`. The internal `PRenderTypes.getTransparencyState(...)` / `isTransparent(...)` helpers use that same material state. Neither alone determines OIT policy. See [Shaders](shaders.md) for the vertex and per-draw inputs.

## Queue stages and scopes

| Stage | Typical owner |
| --- | --- |
| `SOLID_BLOCKS` | Block-entity geometry |
| `TRANSLUCENT_BLOCKS` | Block-entity transparency |
| `ENTITIES` | World entities and items outside first person |
| `FIRST_PERSON` | Physical left/right first-person item and attachment rendering |
| `GUI` | Item display context inside an isolated GUI scope |

World hooks flush opaque entity/block geometry after collection, capture native opaque depth, then compile world transparency together after particles. Deferred OIT composition follows the active native transparency/weather route. [`PRenderStagesHandler`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java) contains shared operations; loader adapters and native hooks own their timing. Mods should not install a second set of world flush callbacks.

First-person geometry is flushed after the hand pass. GUI and offscreen [`PRenderContexts`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderContexts.java) own local compilers and restore captured output state on close. They never consume the world compiler's pending geometry or OIT. Establish local scopes around execution, after selecting output attachments; collector extraction alone does not establish an execution lifetime.

## Manual submission

For a baked mesh, the facade accepts a copied instance transform and an optional deformation:

```java
PRenderQueue.submit(
		PRenderQueue.RenderStage.ENTITIES,
		renderType,
		bakedMesh,
		null,
		new PRenderQueue.InstanceData(matrix, 0xFFFFFFFF, packedLight, packedOverlay),
		PTransparencyMode.ORDINARY);
```

Portable submissions use `PGeometryData` or `PDynamicGeometry` with a `PInstanceHeader`. Geometry uses the same triangle layout, with unsigned 16-bit or 32-bit indices. Reuse immutable geometry objects so the registry can reuse their arena allocation.

For changing geometry, reuse one `PGlDynamicGeometry` and call `update(PGeometryData)` on the render thread. Queued work resolves that source at execution time; flush before changing its index count or layout. Close caller-owned dynamic sources when finished. Do not pass a retired vanilla `VertexBuffer` to the queue.

## Compiled plans and resource ownership

`PFrameCompiler<S>` produces a `PRenderPlan` containing `PPipelineHandle` / `PMeshHandle`, indexed `PDrawCommand` values and packed `PInstanceHeader` data. Combined compilation sorts ordinary transparency across the requested stages and keeps OIT groups separate. `compileOpaque(...)` consumes only opaque submissions.

`PGlMultiDrawExecutor` executes plans using the 26.1 geometry arena, instance streams, indirect submission and OIT implementation. Canonical `legacy.Gl*` classes delegate to this same backend. They do not create another renderer. `PRenderFrame` captures model-view/projection state; an external host can supply its own frame. `PRenderCapabilities` reports pose storage, upload and draw modes independently; CPU pose evaluation can coexist with GPU mesh deformation.

Executor cleanup releases its execution resources. A separately owned `PGlResourceRegistry` retains registered geometry until its own `clear()` or source close; custom hosts must manage both lifetimes. The normal loader reload/disconnect/shutdown integration already owns the library's cleanup.

## Renderer hooks

The built-in renderers retain owner-based customization and add native render-state overloads. Their native submit lifecycle is `preSubmit(...)`, `trueSubmit(...)`, then `postSubmit(...)`:

```java
@Override
public void postSubmit(PoseStack poseStack,
					   RobotRenderState renderState,
					   CameraRenderState cameraRenderState,
					   SubmitNodeCollector submitNodeCollector)
{
	// Submit extra PulseLib meshes or native collector nodes here.
}
```

Use the supplied render state and invocation inputs when preparing submissions. Capture any values needed by deferred collector callbacks before queuing them. See [Renderers](renderers.md) and the [API reference](api-reference.md).
