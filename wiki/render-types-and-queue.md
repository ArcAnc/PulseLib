# Render Types and Queue

PulseLib models are triangle meshes. [`PRenderTypes`](../common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java) supplies their native render pipelines and material states; [`PRenderQueue`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java) compiles captured submissions and executes them through the shared GPU backend.

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

World OIT requires an opaque depth attachment with the same dimensions as the color target. The compositor keeps four peeled layers and uses Minecraft’s active transparency targets. GUI and offscreen scopes flush local transparency using sorted alpha.

## Pipeline contract

Minecraft's native type is `net.minecraft.client.renderer.rendertype.RenderType`. Use the built-in factories for PulseLib draws on both OpenGL and Vulkan. They select `PrimitiveTopology.TRIANGLES`, `POSITION_TEX_NORMAL`, the matching `InstanceData` or `ColorOverlay` binding, and the appropriate blend, depth and target state. The native executors select supported pipeline variants by their registered identities. Matching the mesh layout alone does not establish support for a custom pipeline. [Shaders](shaders.md)

`RenderPipeline.getColorTargetState().blendFunction()` exposes the native blend state. `PRenderTypes.getTransparencyState(...)` and `isTransparent(...)` use that state. OIT policy is selected separately at submission.

## Queue stages and scopes

| Stage | Typical owner |
| --- | --- |
| `SOLID_BLOCKS` | Block-entity geometry |
| `TRANSLUCENT_BLOCKS` | Block-entity transparency |
| `ENTITIES` | World entities and items outside first person |
| `FIRST_PERSON` | Physical left/right first-person item and attachment rendering |
| `GUI` | Item display context inside an isolated GUI scope |

The world hooks flush PulseLib depth writers after the native opaque features and capture opaque depth after the native feature batches. After translucent particles they sort ordinary alpha across the world stages and prepare OIT groups. A native framegraph pass inserts the OIT layers into the active transparency route before always-on-top rendering. The loader integration owns these flushes. [World stage handlers](../common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java) · [Framegraph pass](../common/src/main/java/com/arcanc/pulselib/content/renderer/PWorldRenderPass.java)

First-person geometry is flushed after the hand pass. GUI and offscreen [`PRenderContexts`](../common/src/main/java/com/arcanc/pulselib/content/renderer/PRenderContexts.java) own local compilers and restore captured output state on close. They never consume the world compiler's pending geometry or OIT. Establish local scopes around execution, after selecting output attachments; collector extraction alone does not establish an execution lifetime.

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

For changing geometry, reuse one `PGlDynamicGeometry` and call `update(PGeometryData)` on the render thread. This compatibility type supplies CPU geometry to the native queue on either backend. Flush before changing the layout or index count of pending geometry. Deferred OIT captures the selected geometry revision when the queue flushes. Close caller-owned sources after their queued work has flushed.

## Compiled plans and resource ownership

`PFrameCompiler<S>` produces a `PRenderPlan` containing `PPipelineHandle` / `PMeshHandle`, indexed `PDrawCommand` values and packed `PInstanceHeader` data. Combined compilation sorts ordinary transparency across the requested stages and keeps OIT groups separate. `compileOpaque(...)` consumes only opaque submissions.

Built-in queued draws execute through Minecraft’s render hardware interface (RHI) on OpenGL and Vulkan. The executor reads packed plans and uses native indexed instancing for queued geometry. The instant path submits individual native draws. `PRenderFrame` captures model-view and projection state; `PRenderCapabilities` describes storage, upload and submission modes. Classes named `PGl*` and `legacy.Gl*` retain compatibility entry points; the built-in queue owns its RHI resources.

The loader integration releases the queue’s GPU resources on reload, disconnect and shutdown. Explicitly managed executors and registries have their own lifetimes. For native backend coverage and the measured cost of ordered alpha draws, see the [26.2 migration validation](../docs/porting/26.2-multiloader.md#этап-8--стабилизация).

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
