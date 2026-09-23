# Shaders

PulseLib has two shader paths. World models normally use the queued, instanced path; GUI rendering and explicit immediate drawing use the immediate path. Both paths consume the same baked `POSITION_TEX_NORMAL` vertex data, but they receive transforms, material data, and deformers differently.

Applications should select a render type through `PRenderTypes.RenderTypeProvider`. Shader instances are registered by PulseLib during NeoForge's `RegisterShadersEvent`; consumers do not need to register the built-in programs themselves.

## Render types and programs

The public render-type name is not always the GLSL program name. Several render types deliberately share one program and differ only in render state or uniform defaults.

| Render type | Normal program JSON | Main use |
| --- | --- | --- |
| `trianglesSolid` | `triangles_solid.json` | Opaque queued world geometry |
| `trianglesCutout` | `triangles_cutout.json` | Queued world geometry with a hard alpha cutoff |
| `trianglesTranslucent` | `triangles_translucent.json` | Queued translucent world geometry; weighted OIT when available |
| `trianglesGui` | `triangles_immediate_lit.json` | Direct GUI drawing with translucent render state |
| `trianglesImmediate` | `triangles_immediate_lit.json` | Direct drawing with opaque render state |

The three queued lit render types share `triangles_instanced_lit.vsh` and `triangles_instanced_lit.fsh`. The cutout and translucent JSON definitions set `AlphaCutout=0.1`; the solid definition uses `0.0` and does not discard by alpha.

Queued instanced and OIT programs use GLSL 330. Immediate and composite programs use GLSL 150.

`trianglesGui` and `trianglesImmediate` share the same immediate shader. Their difference is the `RenderType` transparency state, not a separate GLSL implementation. The immediate program also uses `AlphaCutout=0.1`.

Each public type has an emissive counterpart:

```java
trianglesSolidEmissive
trianglesCutoutEmissive
trianglesTranslucentEmissive
trianglesGuiEmissive
trianglesImmediateEmissive
```

Queued emissive types use `triangles_instanced_emissive`; direct types use `triangles_immediate_emissive`. Emissive fragments still apply texture alpha, any configured alpha cutoff, instance tint, and overlay, but they do not apply directional lighting or the lightmap.

## Queued instanced path

The queued path is used by normal entity, block-entity, and non-GUI item rendering. `GlDrawExecutor` binds static geometry from the geometry arena or a dynamic vertex buffer, then supplies per-instance attributes at locations 4 through 10.

| Location | Input | Meaning |
| --- | --- | --- |
| 0 | `Position` | Local vertex position |
| 1 | `UV0` | Runtime-atlas texture coordinates |
| 2 | `Normal` | Local packed normal |
| 4-6 | `InstanceRow0..2` | Affine instance transform |
| 7 | `InstanceColor` | ARGB tint converted to normalized floats |
| 8 | `InstanceLight` | Block and sky light coordinates |
| 9 | `InstanceOverlay` | Minecraft overlay coordinates |
| 10 | `InstanceDeformer` | Operation offset, value offset, and operation count |

Location 3 is intentionally unused. The instance transform is stored as three `vec4` rows and reconstructed by `instanced_transform.glsl`. The shader applies GPU deformers in local mesh space before applying the instance, model-view, and projection matrices.

The instanced programs require these samplers:

| Sampler | Contents |
| --- | --- |
| `Sampler0` | PulseLib runtime texture atlas |
| `Sampler1` | Minecraft overlay texture |
| `Sampler2` | Minecraft lightmap; lit variants only |
| `DeformerOperations` | Static `samplerBuffer` containing compiled deformer operations |
| `DeformerValues` | Per-frame `samplerBuffer` containing current channel values |

`ModelViewMat` and `ProjMat` are set for each draw. Lit variants also use `Light0_Direction` and `Light1_Direction`. Lighting is two-sided: front and back light values are calculated in the vertex shader and selected through `gl_FrontFacing` in the fragment shader.

## Immediate path

Immediate rendering is used for GUI models and by APIs that call `PBakedModel.instantDraw`. It does not use the instance buffer, multi-draw commands, or shader-side mesh deformers. Any deformation required by this path is resolved into a CPU-side dynamic vertex buffer first.

Instead of instance attributes, the immediate lit shader receives:

* `ModelViewMat`, `ProjMat`, and `NormalMat`;
* `Color`;
* `Light` and `Overlay`;
* `Sampler0`, `Sampler1`, and `Sampler2`.

The immediate emissive shader omits `NormalMat`, `Light`, `Sampler2`, and directional-light uniforms because it does not evaluate lighting.

## Alpha modes

`PAlphaMode` maps materials to the queued render types:

| Alpha mode | Render type |
| --- | --- |
| `OPAQUE` | `trianglesSolid` |
| `CUTOUT` | `trianglesCutout` |
| `TRANSLUCENT` | `trianglesTranslucent` |

`AUTO` is resolved from the atlas sprite before a render type is selected. A texture containing only alpha values 0 and 255 becomes `CUTOUT`; any alpha value from 1 through 254 makes it `TRANSLUCENT`; a fully opaque texture becomes `OPAQUE`.

The renderer's constructor-provided render type remains authoritative by default. Call `withAlphaMode(PAlphaMode.AUTO)` from a bone or mesh render resolver to opt into the baked texture classification, or pass a concrete mode to force a specific path. See [Resources](resources.md) for metadata and examples.

Alpha-mode selection currently targets the queued world path: it resolves to `trianglesSolid`, `trianglesCutout`, or `trianglesTranslucent`. Do not apply `withAlphaMode(...)` to a GUI/immediate context, because those render types require the immediate uniform contract rather than queued instance attributes.

## Weighted blended OIT

`trianglesTranslucent` and `trianglesTranslucentEmissive` are marked as OIT-capable render types. When independent blending is available through OpenGL 4.0 or `ARB_draw_buffers_blend`, `GlDrawExecutor` uses four depth-peeled layers. For every layer it first renders the nearest visible transparent fragment depth, then accumulates only fragments matching that depth. Each layer owns:

* an `RGBA16F` accumulation texture;
* an `R16F` revealage texture.
* an `R32F` transparent depth texture.

`triangles_oit_depth*.fsh` produce each layer's depth. `triangles_oit*.fsh` and their emissive variants sample `LayerDepthSampler` and write weighted color and revealage only for the matching layer. `oit_composite` resolves non-empty layers from farthest to nearest after weather. It composites into Minecraft's weather target under Fabulous graphics so the result participates in the vanilla transparency chain, and into the main target otherwise. The OIT framebuffer shares the active target's depth texture, but translucent draws do not write depth.

The same OIT programs are used for first-person PulseLib items. Their stage is resolved immediately after Minecraft renders the hands, rather than being retained until the world transparency pass.

If OIT is unsupported, its framebuffer cannot be created, or the OIT shaders are unavailable, PulseLib falls back to the normal translucent program. OIT shader selection is internal; application code should continue to request `trianglesTranslucent` rather than using `triangles_oit` directly.

Custom translucent render types are not automatically marked as OIT-capable. They use the sorted fallback unless the backend gains an explicit registration API for them.

## Shared shader includes

The files under `assets/pulselib/shaders/include` divide reusable shader behavior by responsibility:

* `deformers.glsl` evaluates the built-in deformation stack and its normal Jacobian;
* `instanced_transform.glsl` reconstructs instance transforms and applies deformation;
* `material_lighting.glsl` calculates two-sided directional light;
* `material_fragment.glsl` samples materials, performs alpha cutoff, overlay, lightmap, and emissive composition;
* `weighted_oit.glsl` calculates OIT weights and render-target outputs.

Use namespaced Mojang imports when reusing them:

```glsl
#moj_import <pulselib:material_fragment.glsl>
```

These files describe the current backend contract, not a stable extension API. A custom queued shader must preserve the vertex format, attribute locations, instance encoding, and expected sampler names. A vanilla shader or a shader written only for `BufferBuilder` data will not receive the instance transform or deformer streams and will render incorrectly through `PRenderQueue`.
