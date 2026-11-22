# Shaders

PulseLib uses two triangle-rendering paths. Normal block-entity, entity, and item rendering is queued and instanced through `PRenderQueue`. Direct `PBakedBone.instantDraw(...)` calls use the instant path. Both consume the same baked `POSITION_TEX_NORMAL` mesh data, but receive per-draw data differently.

PulseLib registers its immutable `RenderPipeline` instances through NeoForge's `RegisterRenderPipelinesEvent` and Fabric client initialization. Both loaders register the GUI picture-in-picture adapter as well. Select render types through `PRenderTypes.RenderTypeProvider`. Every current core program uses GLSL version 330. [Render type factories](../common/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java)

## Built-in shader paths

| Path | Shader sources | Used by |
| --- | --- | --- |
| Queued | [`triangles.vsh`](../common/src/main/resources/assets/pulselib/shaders/core/triangles.vsh) and [`triangles.fsh`](../common/src/main/resources/assets/pulselib/shaders/core/triangles.fsh) | `PBlockRenderer`, `PEntityRenderer`, and `PItemRenderer`, including GUI items |
| Instant | [`triangles_instant.vsh`](../common/src/main/resources/assets/pulselib/shaders/core/triangles_instant.vsh) and [`triangles_instant.fsh`](../common/src/main/resources/assets/pulselib/shaders/core/triangles_instant.fsh) | `PBakedBone.instantDraw(...)` and `PLibHelper.renderModelInGui(...)` |
| OIT | [`triangles_oit.fsh`](../common/src/main/resources/assets/pulselib/shaders/core/triangles_oit.fsh), [`triangles_oit_depth.fsh`](../common/src/main/resources/assets/pulselib/shaders/core/triangles_oit_depth.fsh), and [`triangles_oit_depth_peel.fsh`](../common/src/main/resources/assets/pulselib/shaders/core/triangles_oit_depth_peel.fsh) | Internal translucent queued rendering |
| OIT composite | [`oit_composite.vsh`](../common/src/main/resources/assets/pulselib/shaders/core/oit_composite.vsh) and [`oit_composite.fsh`](../common/src/main/resources/assets/pulselib/shaders/core/oit_composite.fsh) | Internal composition of translucent layers |
| Fabulous OIT | [`oit_fabulous_metadata.fsh`](../common/src/main/resources/assets/pulselib/shaders/core/oit_fabulous_metadata.fsh) and [`oit_fabulous_composite.fsh`](../common/src/main/resources/assets/pulselib/shaders/core/oit_fabulous_composite.fsh) | Depth-aware insertion into Minecraft’s transparency chain |

The queued and instant paths select their solid, cutout, translucent, and emissive variants through pipeline defines. `FORCE_OPAQUE` forces output alpha to one, `ALPHA_CUTOUT` discards texels below `0.1`, and `EMISSIVE` bypasses directional lighting and the lightmap while retaining texture alpha, tint, and overlay.

## Render types

Use these public factories as renderer inputs:

| Material result | Queued renderer | Direct `instantDraw` |
| --- | --- | --- |
| Opaque | `trianglesSolid` | `trianglesInstantSolid` |
| Cutout | `trianglesCutout` | `trianglesInstantCutout` |
| Translucent | `trianglesTranslucent` | `trianglesInstantTranslucent` |
| Emissive | `trianglesEmissiveSolid`, `trianglesEmissiveCutout`, `trianglesEmissiveTranslucent` | matching `trianglesInstantEmissive...` variant |

Use `trianglesInstantTranslucent` for direct drawing. A `PItemRenderer` remains on the queued path even in a GUI. `PAlphaMode` maps to the queued solid, cutout, and translucent variants, while `forInstantAlphaMode(...)` selects the instant equivalents. See [Resources](resources.md#alpha-modes) for texture alpha classification.

## Vertex and per-draw contract

Mesh shaders use `PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL` with `PrimitiveTopology.TRIANGLES`. The vertex is 24 bytes with `Position` (3 floats), `UV0` (2 floats), and `Normal` (4 packed signed-normalized bytes).

The queued shader indexes the std140 `InstanceData` uniform block with `gl_InstanceID`. Each record is 112 bytes. Built-in queued shaders declare 128 records, and the executor binds a complete aligned block even for a short batch. [GLSL layout](../common/src/main/resources/assets/pulselib/shaders/include/instances.glsl) · [Java layout](../common/src/main/java/com/arcanc/pulselib/content/renderer/rhi/PRhiInstanceLayout.java)

| Field | Type | Meaning |
| --- | --- | --- |
| `InstanceModel` | `mat4` | Instance transform composed with the captured model-view matrix |
| `InstanceColor` | `vec4` | Normalized RGBA from the packed ARGB tint |
| `InstanceLight` | `vec2` | Block and sky light |
| `InstanceOverlay` | `vec2` | Overlay coordinates |
| `InstanceDeformer` | `ivec4` | Operation offset, value offset, operation count, padding |

The instant shader uses the per-draw `ColorOverlay` uniform block for color, light, overlay and deformer offsets/count. Both paths use Minecraft’s matrices and projection bindings, the runtime atlas as `Sampler0`, and the appropriate overlay and lightmap samplers.

## GPU deformers

[`deformers.glsl`](../common/src/main/resources/assets/pulselib/shaders/include/deformers.glsl) applies the compiled operation stack in local mesh space before the model transform. It reads `DeformerOperations` and `DeformerValues` texel buffers and updates normals from the deformation Jacobian. Both queued and instant paths use this include; the mesh can also be subdivided before submission. Built-in supported stacks use shader deformation; unsupported/custom prepared operations use CPU-deformed dynamic geometry through the same backend.

The shader supports up to eight operations per submitted mesh. The native bind groups declare both deformer texel buffers with `GpuFormat.RGBA32_FLOAT`. These bindings and the instance layout are internal to the built-in pipelines.

## Translucency and OIT

`trianglesTranslucent` and `trianglesEmissiveTranslucent` support `PTransparencyMode.OIT` with a matching opaque depth attachment. PulseLib peels four layers using reversed-Z depth targets, accumulates weighted color and revealage per layer, then composites back-to-front. The Fabulous route also reads the native terrain, item/entity, particle, weather and cloud targets.

`PTransparencyMode.ORDINARY` keeps the same material and uses sorted alpha blending. Queue overloads without a policy default supported built-in queued translucent types to OIT; callers such as living attachments may explicitly request ordinary alpha. Custom translucent types are not automatically OIT-enabled. Application code selects a normal material and policy; it does not bind internal OIT pass shaders.

For render stages, queue batching, and the normal renderer entry points, see [Render types and queue](render-types-and-queue.md).
