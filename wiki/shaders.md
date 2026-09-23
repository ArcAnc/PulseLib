# Shaders

PulseLib renders its baked triangle meshes through NeoForge's RHI `RenderPipeline` API. Normal block, entity, and non-GUI item rendering goes through the queued path; GUI rendering and direct `PBakedBone.instantDraw(...)` calls use the instant path. Both consume the same `POSITION_TEX_NORMAL` mesh data from the runtime atlas, but receive their per-draw data differently.

Choose a built-in render type through [`PRenderTypes`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java). PulseLib registers its pipelines during `RegisterRenderPipelinesEvent`; consumers do not register the built-in pipelines or compile shader files themselves.

## Built-in render types

The public render-type name and the GLSL source are separate concerns. Most variants share `triangles.vsh` and `triangles.fsh`, or their instant equivalents, and differ through pipeline state and shader defines.

| Path | Render types | Shader sources | Use |
| --- | --- | --- | --- |
| Queued | `trianglesSolid`, `trianglesCutout`, `trianglesTranslucent` | `core/triangles.vsh`, `core/triangles.fsh` | World, entity, and non-GUI item meshes submitted through `PRenderQueue` |
| Queued emissive | `trianglesEmissiveSolid`, `trianglesEmissiveCutout`, `trianglesEmissiveTranslucent` | `core/triangles.vsh`, `core/triangles.fsh` with `EMISSIVE` | Queued meshes whose material is emissive |
| Instant | `trianglesInstantSolid`, `trianglesInstantCutout`, `trianglesInstantTranslucent` | `core/triangles_instant.vsh`, `core/triangles_instant.fsh` | Direct `instantDraw(...)` rendering, including GUI item models |
| Instant emissive | `trianglesInstantEmissiveSolid`, `trianglesInstantEmissiveCutout`, `trianglesInstantEmissiveTranslucent` | `core/triangles_instant.vsh`, `core/triangles_instant.fsh` with `EMISSIVE` | Emissive meshes on the instant path |

`trianglesGui` remains a compatibility alias for `trianglesInstantTranslucent`; it does not have its own shader. The GUI item renderer uses a custom-geometry callback to run the instant path, then selects each mesh's instant variant from its resolved material.

All built-in triangle pipelines disable face culling. Their alpha and lighting variants are controlled by defines:

| Define | Effect |
| --- | --- |
| `OPAQUE` | Forces the fragment alpha to `1.0`. |
| `ALPHA_CUTOUT=0.1` | Discards fragments with lower texture alpha. |
| `EMISSIVE` | Skips directional lighting and lightmap multiplication while retaining texture colour, tint, alpha handling, and overlay. |

The solid variants use `OPAQUE`. Cutout variants use `ALPHA_CUTOUT`; translucent variants also enable normal alpha blending and disable depth writes. The fragment shader chooses a separately lit front or back colour with `gl_FrontFacing`, so lighting remains two-sided.

## Queued path

[`PRenderQueue`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java) groups compatible meshes, and `RhiDrawExecutor` draws each group as instances. The only vertex attributes are `Position`, `UV0`, and `Normal`. Per-instance data is packed into the `std140` `InstanceData` uniform block and read with `gl_InstanceID`.

| `InstanceData` field | Source |
| --- | --- |
| `InstanceModel` | The mesh pose matrix |
| `InstanceColor` | ARGB tint converted to normalized RGBA |
| `InstanceLight` | Packed block and sky light coordinates |
| `InstanceOverlay` | Packed Minecraft overlay coordinates |
| `InstanceDeformer` | Deformer operation offset, value offset, and operation count |

One shader invocation accepts up to 512 instances; larger groups are split into multiple draws. The vertex shader applies GPU deformation in model space, applies `InstanceModel`, then applies Minecraft's model-view and projection matrices.

The queued shaders receive the runtime atlas, overlay, lightmap, and deformer streams:

| Resource | Purpose |
| --- | --- |
| `Sampler0` | PulseLib runtime texture atlas |
| `Sampler1` | Minecraft overlay texture |
| `Sampler2` | Minecraft lightmap for lit variants |
| `DeformerOperations` | Texture buffer containing compiled deformer definitions |
| `DeformerValues` | Texture buffer containing the current per-frame deformer values |

Lit variants calculate directional light in the vertex shader and multiply the resulting fragment colour by the Minecraft lightmap. Emissive variants omit both operations.

## Instant path

`PBakedBone.instantDraw(...)` bypasses the queue and renders a mesh directly. It uses the same triangle vertex format, runtime atlas, overlay texture, lightmap, and deformer texture buffers as the queued path. Its per-mesh values are supplied in the `ColorOverlay` uniform block:

| `ColorOverlay` field | Purpose |
| --- | --- |
| `Color` | ARGB mesh tint |
| `Light` | Block and sky light coordinates |
| `Overlay` | Minecraft overlay coordinates |
| `InstanceDeformer` | Deformer operation offset, value offset, and operation count |

GPU deformers run in the instant vertex shader as well. When a requested deformation cannot run on the GPU, PulseLib first creates a CPU-deformed mesh buffer and draws that buffer with the same pipeline.

`PMeshRenderMaterial.resolveInstantRenderType(...)` converts the renderer's base type to its instant solid, cutout, or translucent equivalent. It also selects an emissive instant variant when the mesh metadata or `PMeshRenderContext` requests one. `withAlphaMode(...)` works on this path too; it chooses an instant variant rather than a queued one.

## Alpha modes and emissive materials

`PAlphaMode` is classified while the model texture is baked, or is forced by texture metadata. A mesh resolver can retain the renderer's base render type, opt into that classification with `withAlphaMode(PAlphaMode.AUTO)`, or choose a concrete mode.

| Alpha mode | Queued type | Instant type |
| --- | --- | --- |
| `OPAQUE` | `trianglesSolid` | `trianglesInstantSolid` |
| `CUTOUT` | `trianglesCutout` | `trianglesInstantCutout` |
| `TRANSLUCENT` | `trianglesTranslucent` | `trianglesInstantTranslucent` |

For `AUTO`, a fully opaque sprite becomes `OPAQUE`; a sprite with only alpha values 0 and 255 becomes `CUTOUT`; intermediate alpha becomes `TRANSLUCENT`. A texture's `pulselib` `.png.mcmeta` may set `alpha_mode` or `emissive`. See [Resources](resources.md) for registration and metadata examples.

## Weighted blended OIT

Only the queued `trianglesTranslucent` and `trianglesEmissiveTranslucent` pipelines are OIT-capable. When their target has a depth attachment, the queue batches them independently of submission order and [`RhiDrawExecutor`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/java/com/arcanc/pulselib/content/renderer/RhiDrawExecutor.java) runs the internal OIT passes.

PulseLib maintains two transparent depth layers. It first records the nearest transparent depth, then peels the next layer behind it. `triangles_oit.fsh` accumulates weighted colour and revealage only for fragments matching the current layer depth. Each layer has an `RGBA16F` accumulation texture, an `R16F` revealage texture, and a `D32` depth texture. `oit_composite.vsh` draws a full-screen triangle, while `oit_composite.fsh` resolves each non-empty layer back onto the destination target.

If the target has no depth attachment or OIT buffer creation fails, PulseLib uses the ordinary translucent pipeline. Custom translucent render types are sorted as normal transparent submissions; they are not automatically eligible for PulseLib's OIT path. Application code should request `trianglesTranslucent`, never the internal `triangles_oit*` or `oit_composite` pipelines.

## Shader files

| File | Responsibility |
| --- | --- |
| [`triangles.vsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/triangles.vsh) | Queued vertex transformation, GPU deformation, directional lighting, lightmap, and overlay setup |
| [`triangles.fsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/triangles.fsh) | Queued material sampling, alpha handling, tint, overlay, lightmap, and emissive output |
| [`triangles_instant.vsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/triangles_instant.vsh) | Instant-path equivalent using `ColorOverlay` |
| [`triangles_instant.fsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/triangles_instant.fsh) | Instant-path material output |
| [`triangles_oit.fsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/triangles_oit.fsh) | Weighted OIT accumulation |
| [`triangles_oit_depth.fsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/triangles_oit_depth.fsh) | First transparent depth layer |
| [`triangles_oit_depth_peel.fsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/triangles_oit_depth_peel.fsh) | Next transparent depth layer |
| [`oit_composite.vsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/oit_composite.vsh) and [`oit_composite.fsh`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/core/oit_composite.fsh) | Full-screen OIT composition |
| [`deformers.glsl`](https://github.com/ArcAnc/PulseLib/blob/master/src/main/resources/assets/pulselib/shaders/include/deformers.glsl) | Built-in GPU deformer stack and normal correction from its Jacobian |

The shader sources and their uniform layouts are an internal backend contract, not a stable shader-extension API. A custom pipeline must keep `POSITION_TEX_NORMAL`, the matching bind-group resources, and the queued or instant uniform layout. A vanilla render type does not provide PulseLib's instance or deformer data and therefore cannot render `PRenderQueue` meshes correctly.
