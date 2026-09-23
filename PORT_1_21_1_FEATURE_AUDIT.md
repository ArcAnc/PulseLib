# 1976c64 feature audit for Minecraft 1.21.1

## Resource architecture

`RuntimeLoader.run` clears `PResourceCache`, posts `RegisterResourceEvent`, and populates model-local `PModelResource` mappings. `PModelCache.loadModels` loads only registered resources; `PMeshRenderResolver`/`PMeshTextureVariants` resolve a material reference through `PResourceCache.resolve(model, reference)` before renderer submission. Thus `textures/claws.png` is keyed by its owning model resource, and two models can map that reference to different texture ids. `PModelLoader.modelResourceCandidates` is used by `PModelCache` to load `.glb` then `.gltf` for extension-less resources, or the opposite extension after an explicit candidate is absent; the thrown diagnostic includes all candidates.

Verification: `PResourceCache.resolve`, `PModelCache.loadModels`, `PModelCache.verifyModelsLoaded`, `PGltfModelLoader.modelResourceCandidates`, and renderer material resolution were inspected. Runtime atlas loading posts the registration event in `RuntimeLoader.run`.

## First-person responsibilities

| Source class / responsibility | 1.21.1 implementation | Verification |
| --- | --- | --- |
| `PFirstPersonArmPose` / arm mode and transform | Same class | validated record invariants |
| `PFirstPersonArmRig` / arm-to-hand rig | Same class | used by presentation builder |
| `PFirstPersonCameraMode` / animated-or-vanilla camera | Same enum | filtered by `PPlayerAnimations.cameraPose` |
| `PFirstPersonCameraSpace` / coordinate conversion | Same class | called by `CameraMixin` |
| `PFirstPersonItemPose` / item mode and transform | Same class | consumed by `ItemInHandRendererMixin` |
| `PFirstPersonItemRig` / item socket rig | Same class | used by item resolver |
| `PFirstPersonPoseStack` / replace final local pose | Same class | arm and item wraps call it |
| `PFirstPersonPresentation` / MODEL-to-VIEW matrices | Same class | built from `PPlayerAnimationFrame` |
| `PFirstPersonRenderContext` | Same class | per-hand context captured in renderer |
| `PFirstPersonRenderContexts` | Same class | scoped around `renderHandsWithItems` |
| `PFirstPersonRenderMode` | Same enum | vanilla, animated, hidden paths inspected |
| `PFirstPersonRenderPresentation` | Same class | combines arms/items/anchors/meshes |
| `PFirstPersonRestPose` | Same class | default arm/item basis |
| `PFirstPersonTransformMode` | Same enum | additive/override item branch |
| `PPlayerFirstPersonAnchorPose` | Same class | dispatched by animated attachments |
| `PPlayerFirstPersonMeshAttachmentPose` | Same class | dispatched by automatic meshes |
| `PPlayerFirstPersonRenderer` | Native `MultiBufferSource` implementation | called before `endBatch` |
| `PPlayerFirstPersonSettings` | Same class | controls activation and camera mode |
| `PVanillaFirstPersonArmResolver` | Same class | final `PlayerRenderer` hand transform |
| `PVanillaFirstPersonItemResolver` | Same class | final `ItemInHandRenderer.renderItem` transform |

Third-person frames remain applied/restored by `LivingEntityRendererMixin`; armor deformers run in `HumanoidArmorLayerMixin` after model animation setup. The direct 1.21.1 item path retains vanilla use/equip/swing and display-context processing before PulseLib transforms its final draw calls.
