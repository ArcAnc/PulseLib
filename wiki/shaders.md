# Shaders

PulseLib uses two triangle-rendering paths. Normal block-entity, entity, and item rendering is queued and instanced through `PRenderQueue`. Direct `PBakedBone.instantDraw(...)` calls use the instant path. Both consume the same baked `POSITION_TEX_NORMAL` mesh data, but receive per-draw data differently.

PulseLib registers its `RenderPipeline` instances itself during NeoForge's `RegisterRenderPipelinesEvent`. Consumers should choose a render type from [`PRenderTypes.RenderTypeProvider`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/PRenderTypes.java), rather than registering or binding a built-in pipeline directly. Every current core program uses GLSL version 330.

## Built-in shader paths

| Path | Shader sources | Used by |
| --- | --- | --- |
| Queued | [`triangles.vsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/triangles.vsh) and [`triangles.fsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/triangles.fsh) | `PBlockRenderer`, `PEntityRenderer`, and `PItemRenderer`, including GUI items |
| Instant | [`triangles_instant.vsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/triangles_instant.vsh) and [`triangles_instant.fsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/triangles_instant.fsh) | `PBakedBone.instantDraw(...)` and automatic player mesh attachments |
| OIT | [`triangles_oit.fsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/triangles_oit.fsh), [`triangles_oit_depth.fsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/triangles_oit_depth.fsh), and [`triangles_oit_depth_peel.fsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/triangles_oit_depth_peel.fsh) | Internal translucent queued rendering |
| OIT composite | [`oit_composite.vsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/oit_composite.vsh) and [`oit_composite.fsh`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/core/oit_composite.fsh) | Internal composition of translucent layers |

The queued and instant paths select their solid, cutout, translucent, and emissive variants through pipeline defines. `FORCE_OPAQUE` forces output alpha to one, `ALPHA_CUTOUT` discards texels below `0.1`, and `EMISSIVE` bypasses directional lighting and the lightmap while retaining texture alpha, tint, and overlay.

## Render types

Use these public factories as renderer inputs:

| Material result | Queued renderer | Direct `instantDraw` |
| --- | --- | --- |
| Opaque | `trianglesSolid` | `trianglesInstantSolid` |
| Cutout | `trianglesCutout` | `trianglesInstantCutout` |
| Translucent | `trianglesTranslucent` | `trianglesInstantTranslucent` |
| Emissive | `trianglesEmissiveSolid`, `trianglesEmissiveCutout`, `trianglesEmissiveTranslucent` | matching `trianglesInstantEmissive...` variant |

`trianglesGui` is a compatibility alias for `trianglesInstantTranslucent`. It is for direct drawing only; a `PItemRenderer` remains on the queued path even in a GUI. `PAlphaMode` maps to the queued solid, cutout, and translucent variants, while `forInstantAlphaMode(...)` selects the instant equivalents. See [Resources](resources.md#alpha-modes) for texture alpha classification.

## Vertex and per-draw contract

All PulseLib mesh shaders use `PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL` with `VertexFormat.Mode.TRIANGLES`:

| Attribute location | Value |
| --- | --- |
| 0 | `Position` — local vertex position |
| 1 | `UV0` — UV in the PulseLib runtime atlas |
| 2 | `Normal` — local packed normal |
| 3 | Unused |

The queued path additionally reads one 96-byte instance record:

| Attribute locations | Value |
| --- | --- |
| 4–6 | `InstanceRow0..2` — affine model transform |
| 7 | `InstanceColor` — normalized ARGB tint |
| 8 | `InstanceLight` — block and sky light coordinates |
| 9 | `InstanceOverlay` — overlay coordinates |
| 10 | `InstanceDeformer` — operation offset, value offset, and operation count |

The instant path has no instance record. It receives colour, light, overlay, and deformer offsets/count through the `ColorOverlay` uniform buffer. Both paths receive the actual deformer streams through `DeformerOperations` and `DeformerValues`, receive Minecraft's dynamic transforms, use the PulseLib runtime atlas as `Sampler0`, and use the overlay and lightmap samplers when the selected variant requires them.

## GPU deformers

[`deformers.glsl`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/resources/assets/pulselib/shaders/include/deformers.glsl) applies the compiled operation stack in local mesh space before the model transform. It reads `DeformerOperations` and `DeformerValues` texel buffers and updates normals from the deformation Jacobian. Both queued and instant paths use this include; instant drawing can also request CPU subdivision before its mesh is submitted.

The shader currently supports up to eight operations per submitted mesh. This is the renderer's internal data contract. A custom pipeline that receives PulseLib geometry must preserve the vertex format, queued attribute locations, and deformer-buffer layout, or it will not render queued meshes correctly.

## Translucency and OIT

`trianglesTranslucent` and `trianglesEmissiveTranslucent` are eligible for PulseLib's internal order-independent transparency path. When the active target has depth and the driver supports OpenGL 4.0 or `ARB_draw_buffers_blend`, PulseLib depth-peels four transparent layers, accumulates weighted colour and revealage for each, then composites them from far to near. The OIT shaders are selected by `PGlMultiDrawExecutor`; application code should continue to request the normal translucent render type.

If that path is unavailable, transparent queued geometry uses the normal translucent pipeline and queue ordering. Custom translucent `RenderType` values are not automatically OIT-enabled.

For render stages, queue batching, and the normal renderer entry points, see [Render types and queue](render-types-and-queue.md).
