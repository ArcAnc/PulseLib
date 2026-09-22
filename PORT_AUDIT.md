# Port audit: 1976c64 source delta

- Source commit: `1976c6423786e38caa9d96c3f385bd469e887e49`
- Source parent: `45d5282`
- Scope: every record from `git diff --name-status -M SOURCE^ SOURCE`; port only that delta.
- Manifest was created before porting; every record is now reconciled below.

## Manifest
### MODEL_RESOURCE (21)

#### `src/main/java/com/arcanc/pulselib/content/model/PBone.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/PMaterial.java`

- Source status: `A`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/PMesh.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/PMeshPrimitive.java`

- Source status: `A`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/PModel.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/PTextureReference.java`

- Source status: `A`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/resource/PModelResource.java`

- Source status: `A`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/resource/package-info.java`

- Source status: `A`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/textures/PAlphaMode.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/textures/PTextureAlphaClassifier.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/textures/atlas/PLibSpriteMetadata.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/PModelLoader.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gltf/PGltfAnimationEventSidecarParser.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Delegates glTF sidecars to `PAnimationSidecarParser`, which now also merges source visibility tracks for `PBakedBone.instantDraw`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gltf/PGltfChannelDecoder.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gltf/PGltfDecodeContext.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelParser.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/PModelCache.java`

- Source status: `M`
- Subsystem: `MODEL_RESOURCE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: bakes material-grouped primitives, resolves textures through the owning model resource, and validates registered-model candidates. 26.2 API difference: GPU buffers use `PrimitiveTopology` and `IndexType`, with the current deformed-buffer cleanup path. 26.2 implementation: retains those 26.2 APIs while applying the resource-scoped bake and fallback validation flow.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/PResourceCache.java`

- Source status: `A`
- Subsystem: `MODEL_RESOURCE`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/PTextureCache.java`

- Source status: `D`
- Subsystem: `MODEL_RESOURCE`
- Port status: `OBSOLETE_WITH_REPLACEMENT`
- Notes: original source responsibility: globally collect texture locations and expose the runtime texture atlas.
  why exact implementation is obsolete in 26.2: the source architecture replaces global texture registration with model-scoped resource registrations.
  replacement file/symbol: `PResourceCache`, `PModelResource`, `PulseLibEvents.RegisterResourceEvent`, and `RuntimeLoader`.
  how equivalent behavior is preserved: the atlas is built only from registered model resources and `PResourceCache.resolve(model, reference)` resolves each texture in that model scope.
- Target path if renamed: `—`

### RENDER_INFRASTRUCTURE (35)

#### `src/main/java/com/arcanc/pulselib/content/model/baked/AtlasBufferBuilder.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `AtlasBufferBuilder.setUv` already implements the inspected behavior.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PBakedBone.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: instant drawing resolves one pose, skips invisible bones, and recurses with that resolver. 26.2 API difference: the current GPU draw path uses `GpuBufferSlice` mapping and RHI render-pass signatures. 26.2 implementation: `PBakedBone.instantDraw(PAnimationPoseResolver, ...)` keeps those APIs while using `PAnimationPoseResolver.isVisible` and `PResourceCache`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PBakedMesh.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PORTED`
- Notes: `PBakedMesh` now carries `textureReference` and `PMeshPrimitive`, preserving resource-scoped primitive baking.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PBakedModel.instantDraw` and `PBakedModel.bindPose` implement the inspected symbols.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderContext.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PMeshRenderContext.withTexture` implements the inspected context override.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderMaterial.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PMeshRenderMaterial.resolve` implements material resolution.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderResolver.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PMeshRenderResolver.resolve` is the inspected resolver contract.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PMeshTextureVariants.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PORTED`
- Notes: `PMeshTextureVariants.resolve` normalizes overrides through `PResourceCache.spriteId`, preventing texture aliases across model resources.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PSubdividedMeshCache.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PORTED`
- Notes: `PSubdividedMeshCache.bake` retains `PMeshPrimitive` source data and reads atlas sprites through `PResourceCache`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PBlockRenderer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PORTED`
- Notes: `PBlockRenderer.submitBone` uses `textureReference` and `PResourceCache.ATLAS_LOCATION` for queued meshes.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderLayer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PEntityRenderLayer.bindBone` and `submit` implement the inspected layer behavior.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PORTED`
- Notes: `PEntityRenderer.submitBone` resolves `textureReference` through `PResourceCache.ATLAS_LOCATION`; deferred-layer submission remains intact.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PORTED`
- Notes: `PItemRenderer.submitBone` and GUI submission use `PResourceCache.ATLAS_LOCATION` with resource-scoped mesh references.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PMeshRenderResolver.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `com.arcanc.pulselib.content.renderer.PMeshRenderResolver.resolve` is the inspected extension contract.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: batches submitted meshes, executes a plan, and composites OIT. 26.2 API difference: `PGlMultiDrawExecutor` and the generic GL plan are removed. 26.2 implementation: `PRenderQueue.flush` uses `RhiDrawExecutor.execute(PFrameCompiler.compile(...))`, whose texture binding uses `PResourceCache`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: flushes solid and translucent render stages then finishes the deformer frame. 26.2 API difference: the RHI queue compiles one stage at a time. 26.2 implementation: `PRenderStagesHandler.renderSolid` and `renderTranslucent` flush the equivalent stages, composite OIT, and call `PGpuDeformerBuffers.finishFrame`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PRenderer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PRenderer.getModelData` and submit hooks implement the inspected contract.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/base/PBlockRenderState.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PBlockRenderState.Impl.extractBlockData` implements the inspected state extraction.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/base/PEntityRenderState.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PEntityRenderState.Impl.extractEntityData` implements the inspected state extraction.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/base/PItemRenderState.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PItemRenderState.Impl.extractStackData` implements the inspected state extraction.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/base/PRenderState.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PRenderState` defines the inspected render-state contract.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlFrameArena.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: owns transient OpenGL frame allocations. 26.2 API difference: the `PGlFrameArena` path was removed for RHI command encoding. 26.2 implementation: `RhiDrawExecutor.execute` submits the compiled frame through 26.2 RHI resources.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlGeometryArena.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: stores GL geometry for multi-draw execution. 26.2 API difference: `PGlGeometryArena` was removed with the GL submission API. 26.2 implementation: `RhiDrawExecutor.execute` binds each `PBakedMesh` RHI buffer directly.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlIndirectStream.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: writes indirect GL commands. 26.2 API difference: `PGlIndirectStream` was removed with the GL submission API. 26.2 implementation: `RhiDrawExecutor.execute` issues indexed RHI draws from `PRenderPlan` groups.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlInstanceStream.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: packs per-instance GL data. 26.2 API difference: `PGlInstanceStream` was removed with the GL submission API. 26.2 implementation: `RhiDrawExecutor.execute` uploads `PRenderQueue.InstanceData` into RHI uniform buffers.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlMultiDrawExecutor.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: executes GL multi-draws and binds the shared texture atlas. 26.2 API difference: `PGlMultiDrawExecutor` was removed. 26.2 implementation: `RhiDrawExecutor.execute` replaces it and binds the atlas from `PResourceCache.getTextureAtlas`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlWeightedBlendedOit.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: accumulates and composites weighted blended OIT. 26.2 API difference: the GL OIT implementation was removed. 26.2 implementation: `RhiDrawExecutor.compositeOit` provides the current RHI OIT composite called by `PRenderQueue.compositeTranslucency`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultBlockModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `DefaultBlockModelData.DefaultBlockModelDataBuilder` implements the inspected builder.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityLayerModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `DefaultEntityLayerModelData.DefaultEntityLayerModelDataBuilder` implements the inspected builder.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `DefaultEntityModelData.DefaultEntityModelDataBuilder` implements the inspected builder.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultItemModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `DefaultItemModelData.DefaultItemModelDataBuilder` implements the inspected builder.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation only; current-master `PModelData.Builder.build` implements the inspected builder flow.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/plan/PDrawGroup.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: stores a pipeline, mesh, depth-write flag, and instances. 26.2 API difference: the GL-generic record is replaced by RHI-specific types. 26.2 implementation: `PDrawGroup` stores `RenderType`, `PBakedMesh`, and `PRenderQueue.InstanceData`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/plan/PFrameCompiler.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: batches opaque draws and sorts transparent draws into a render plan. 26.2 API difference: generic GL planning is replaced by RHI types and OIT-aware batching. 26.2 implementation: `PFrameCompiler.compile` produces `PDrawGroup` instances consumed by `RhiDrawExecutor.execute`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/plan/PRenderPlan.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: exposes an immutable list of planned draw groups. 26.2 API difference: the generic GL plan is replaced by RHI-specific groups. 26.2 implementation: `PRenderPlan` and `PRenderPlan.EMPTY` provide the immutable RHI plan consumed by `RhiDrawExecutor.execute`.
- Target path if renamed: `—`

### ANIMATION_CORE (56)

#### `src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/ControllerState.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationCameraShake.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventCallbacks.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventDispatcher.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationManager.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PLibAnimationTicker.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/instance/InstanceAnimationManager.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/BoneFrame.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Dependency required by rendering: carries immutable visibility tracks and exposes `isBoneVisible` for the baked-bone pose resolver.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannel.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannelType.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationDecodeContext.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvaluationContext.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvent.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventContext.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventType.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventTypes.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationFormatDecoder.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraph.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraphRuntime.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationParameters.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationPoseResolver.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Source delta applied: resolver now evaluates visibility, exposes active animation bones, and resolves local/model/relative transforms through `PTransform`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationRuntime.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationSample.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationState.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTrack.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTransition.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationType.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationValue.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationVisibilityTrack.java`

- Source status: `A`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Dependency required by rendering: ordered visibility keyframes resolve the current bone visibility.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PBlendMode.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PBoneAnimation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PCompiledAnimation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PCondition.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PEventSide.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolationType.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PInterruptionPolicy.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PKeyframe.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PModelPose.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPose.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPoseBlendMode.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPoseEasing.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPoseMixer.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPoseWriter.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PRawAnimation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionDelta.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionRuntime.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PTransform.java`

- Source status: `A`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PTransitionInterruptionPolicy.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PVectorConversion.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/PAnimationSidecarParser.java`

- Source status: `A`
- Subsystem: `ANIMATION_CORE`
- Port status: `PORTED`
- Notes: Dependency required by rendering: parses and merges sidecar visibility tracks alongside animation events.
- Target path if renamed: `—`

### PLAYER_ANIMATION (18)

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchor.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchorPose.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchors.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationBlendMode.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDefinition.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Foundation bindings, anchors and mesh-attachment roots ported. First-person configuration members remain deferred with the FIRST_PERSON stage.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformer.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerApplication.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerContext.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerValueSource.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationFrame.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationHandle.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationInstance.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Shared activation and cached evaluated-frame runtime ported. First-person activation state remains deferred with the FIRST_PERSON stage.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationMask.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationSpace.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationWeight.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimations.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Third-person frame evaluation, anchors, mesh attachments and deformers ported. First-person presentation hooks remain deferred with the FIRST_PERSON stage.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerBonePose.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerPart.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `ALREADY_PRESENT`
- Notes: Current `resolve(PlayerModel)` already maps only base model parts, matching the source change.
- Target path if renamed: `—`

### FIRST_PERSON (21)

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmRig.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraMode.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraSpace.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemRig.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPoseStack.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPresentation.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContext.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContexts.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderMode.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderPresentation.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRestPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonTransformMode.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonAnchorPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonMeshAttachmentPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonRenderer.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonSettings.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonArmResolver.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonItemResolver.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/package-info.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

### MIXIN_INTEGRATION (18)

#### `src/main/java/com/arcanc/pulselib/content/mixin/BlockEntityRenderStateAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/CameraMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: applies animated first-person camera position and rotation.
  26.2 API difference: camera alignment and renderer submission use the 26.2 client signatures.
  26.2 implementation: `CameraMixin#pulselib$followAnimatedHead` uses `PFirstPersonCameraSpace` after `Camera.alignWithEntity`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/CubeDefinitionMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/EntityModelSetMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/GameRendererMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: flushes PulseLib first-person work at the hand-pass boundary.
  26.2 API difference: feature submission is dispatched by `FeatureRenderDispatcher.renderAllFeatures`.
  26.2 implementation: the current `GameRendererMixin` targets that 26.2 dispatch point while `ItemInHandRendererMixin` owns hand submission.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/GlBufferAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/HumanoidArmorLayerMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererAccessor.java`

- Source status: `D`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `OBSOLETE_WITH_REPLACEMENT`
- Notes: original source responsibility: invoke the old private arm method for animated first-person arms.
  why exact implementation is obsolete in 26.2: the source rewrite and 26.2 `AvatarRenderer` hand submission make the private invoker neither used nor registered.
  replacement file/symbol: `ItemInHandRendererMixin#pulselib$renderRightHand`, `#pulselib$renderLeftHand`, and `#renderHeldArm`.
  how equivalent behavior is preserved: the replacement wraps final 26.2 hand submissions and applies the same resolved arm pose.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: resolves separate arm/item poses, suppresses hidden channels, transforms animated channels, and renders attachments.
  26.2 API difference: `renderHandsWithItems`/`renderArmWithItem` became `submitHandsWithItems`/`submitArmWithItem`, and hand methods take erased `Avatar` parameters.
  26.2 implementation: `ItemInHandRendererMixin` wraps the 26.2 submit methods and `AvatarRenderer.renderRightHand/renderLeftHand`, preserving the source presentation semantics.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ItemModelResolverAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ItemStackRenderStateAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/LivingEntityRendererMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ModelPartCubesMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/PlayerRendererMixin.java`

- Source status: `D`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `OBSOLETE_WITH_REPLACEMENT`
- Notes: original source responsibility: inject animated first-person arm pose and attachment rendering into `AvatarRenderer.renderHand`.
  why exact implementation is obsolete in 26.2: the source rewrite moves this ownership to the hand/item submission pipeline, and 26.2 no longer uses that source injection shape.
  replacement file/symbol: `ItemInHandRendererMixin`, `PPlayerFirstPersonRenderer#renderExtras`, and `PFirstPersonRenderContexts`.
  how equivalent behavior is preserved: arm transforms, hidden/vanilla/animated modes, mesh attachments, and persistent attachments are resolved before final 26.2 submissions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/PlayerRootTransformMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation-only here; current-master `PlayerRootTransformMixin#pulselib$applyPlayerRoot` and `#pulselib$restorePlayerRoot` already implement the inspected root transform behavior.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperRenderStateExtractor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/resources/pulselib.mixins.json`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: registers the first-person rewrite hooks and removes superseded hooks.
  26.2 API difference: the retained 26.2 `BlockEntityMixin` remains registered in addition to the source list.
  26.2 implementation: registers `GlBufferAccessor` and the 26.2 hand/camera hooks; obsolete old first-person mixins are excluded.
- Target path if renamed: `—`

### ATTACHMENTS_AND_DEFORMERS (56)

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PChannelReference.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerFrame.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerInstance.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerPrepareContext.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerStack.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `ALREADY_PRESENT`
- Notes: Current `frameAt`, `deformNormal` and `compile` implement the source behavior; the remaining delta is documentation.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerValueSource.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `ALREADY_PRESENT`
- Notes: Current `PHingeDeformer.Operation.deform` implements the source behavior; the remaining delta is documentation.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformation.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshTessellator.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `ALREADY_PRESENT`
- Notes: Current `subdivide(PMeshPrimitive, int)` already preserves the source primitive/material conversion.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PPreparedDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `ALREADY_PRESENT`
- Notes: Current `PSquashDeformer.Operation.deform` implements the source behavior; the remaining delta is documentation.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `ALREADY_PRESENT`
- Notes: Current `PTwistDeformer.Operation.deform` implements the source behavior; the remaining delta is documentation.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PDeformerStream.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerBuffers.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `ALREADY_PRESENT`
- Notes: Current `UploadBuffers.upload` implements the source buffer lifecycle; the remaining delta is documentation.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerStack.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentContext.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentLayer.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: renders automatic mesh attachments and registered anchor attachments in the player layer.
  26.2 API difference: mesh attachment rendering must use the collector passed to `RenderLayer.submit`.
  26.2 implementation: forwards `SubmitNodeCollector` to `PPlayerAutomaticMeshAttachments`, preserving the player id, partial tick, packed light, and registered-anchor rendering.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentRenderer.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachments.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimationMeshAttachmentPose.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAutomaticMeshAttachments.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: draws evaluated third-person mesh attachment roots directly from the render layer pose.
  26.2 API difference: `RenderLayer.submit` is a submission-collection phase, so immediate GPU work is not associated with its saved entity transform.
  26.2 implementation: `renderThirdPerson` submits custom geometry through `SubmitNodeCollector`, captures the submitted pose, and performs the same evaluated `PBakedBone.instantDraw` in the renderer pass.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/package-info.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformableCubeBakeScope.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformedCuboid.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PModelPartCubes.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerDeformerValueSource.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerMeshDeformers.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerVertexDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchor.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentBinding.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentLayer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSource.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSources.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachments.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolver.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolvers.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PTransform.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAttachmentLayer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

### DEMO_TEST_RESOURCES (20)

#### `src/main/java/com/arcanc/pulselib/content/registration/PLibRegistration.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/PRegistry.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/block/TestBlock.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/TestBlockEntity.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/TestBlockEntityRenderer.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/renderState/TestBlockEntityRenderState.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/entity/TestEntity.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/PTestArmor.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/TestEntityRender.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is imports and documentation only; current-master `TestEntityRender#resolveMeshRender` already provides the inspected mesh/deformer behavior.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/item/TestArmorItem.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/item/TestBlockItem.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/item/renderer/TestBlockItemRenderer.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/item/renderer/renderState/TestBlockItemRenderState.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerAcrobaticDemo.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerBallDemo.java`

- Source status: `A`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/renderer/TestDayTimeColor.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/resources/assets/pulselib/glmodels.zip`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/resources/assets/pulselib/template/player/player_model_template.bbmodel`

- Source status: `A`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/resources/assets/pulselib/template/player/player_model_template.gltf`

- Source status: `A`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/resources/assets/pulselib/textures.zip`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

### DOCUMENTATION (12)

#### `changelog.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/api-reference.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/armor-and-attachments.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/basic.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/installation.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/model-loaders.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/modeldata.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/player-animations.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/pulselib-items.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/render-types-and-queue.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/renderers.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `wiki/resources.md`

- Source status: `R`
- Subsystem: `DOCUMENTATION`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `wiki/resources.md`

### BUILD_METADATA (1)

#### `gradle.properties`

- Source status: `M`
- Subsystem: `BUILD_METADATA`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: updates the PulseLib module version.
  26.2 API difference: current master targets Minecraft 26.2 and NeoForge 26.2.
  26.2 implementation: `mod_version` is 1.1.5 while `minecraft_version=26.2`, `minecraft_version_range=[26.2]`, and `neo_version=26.2.0.57` are retained.
- Target path if renamed: `—`

### OTHER (19)

#### `src/main/java/com/arcanc/pulselib/content/event/ClientEvents.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: registers `PResourceCache` and posts/applies animated-attachment registration.
  26.2 API difference: client bootstrap uses current 26.2 lifecycle events.
  26.2 implementation: `registerClientEvents` registers `PResourceCache`; `ensurePulseClientContentRegistered` posts and applies `PlayerAnimatedAttachmentRegistrationEvent`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/event/CommonEvents.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: provides resource registration and animated-player-attachment registration.
  26.2 API difference: registrations are consumed by current 26.2 client lifecycle events.
  26.2 implementation: `RegisterResourceEvent` builds `PModelResource` values and `PlayerAnimatedAttachmentRegistrationEvent` queues `PPlayerAnimatedAttachments.register`.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/MolangParser.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PExpressionDependency.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PExpressionEvaluator.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoAnimationEventParser.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoChannelDecoder.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoDecodeContext.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelParser.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PMolangEulerRotationValue.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PMolangVectorValue.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/PLibDatabase.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/PRenderTypes.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `ALREADY_PRESENT`
- Notes: Source delta is documentation-only; current-master `PRenderTypes.RenderTypeProvider` already provides every inspected pipeline and variant method.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/helpers/PLibCodecs.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/helpers/PLibParserHelper.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/helpers/PLibRenderHelper.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PORTED`
- Notes: Source delta was reviewed against 26.2 and ported without changing current Minecraft, NeoForge, Gradle, Java, or dependency versions.
- Target path if renamed: `—`

## Reconciliation

| Subsystem | Files |
| --- | ---: |
| MODEL_RESOURCE | 21 |
| RENDER_INFRASTRUCTURE | 35 |
| ANIMATION_CORE | 56 |
| PLAYER_ANIMATION | 18 |
| FIRST_PERSON | 21 |
| MIXIN_INTEGRATION | 18 |
| ATTACHMENTS_AND_DEFORMERS | 56 |
| DEMO_TEST_RESOURCES | 20 |
| DOCUMENTATION | 12 |
| BUILD_METADATA | 1 |
| OTHER | 19 |
| **Total source paths** | **277** |

## Functional feature audit

### First-person animation rewrite

#### SOURCE FEATURE

Rewrite whole first-person animations: separate arm and item poses, camera space and mode, presentations, rest poses, transform modes, anchor/mesh attachment poses, vanilla resolvers, renderer, and settings.

#### 26.2 IMPLEMENTATION

`PFirstPersonPresentation` builds a canonical model-to-view presentation from `PPlayerAnimationFrame`. `PFirstPersonRenderContexts` scopes it per 26.2 hand submission. `ItemInHandRendererMixin` applies `VANILLA`, `ANIMATED`, and `HIDDEN` channel modes at `submitHandsWithItems`/`submitArmWithItem`, while `CameraMixin` applies first-person camera space.

#### FILES/SYMBOLS

`PFirstPersonArmPose`, `PFirstPersonArmRig`, `PFirstPersonCameraMode`, `PFirstPersonCameraSpace`, `PFirstPersonItemPose`, `PFirstPersonItemRig`, `PFirstPersonPoseStack`, `PFirstPersonPresentation`, `PFirstPersonRenderContext`, `PFirstPersonRenderContexts`, `PFirstPersonRenderMode`, `PFirstPersonRenderPresentation`, `PFirstPersonRestPose`, `PFirstPersonTransformMode`, `PPlayerFirstPersonAnchorPose`, `PPlayerFirstPersonMeshAttachmentPose`, `PPlayerFirstPersonRenderer`, `PPlayerFirstPersonSettings`, `PVanillaFirstPersonArmResolver`, and `PVanillaFirstPersonItemResolver` are present at their source responsibilities. The 26.2 boundary implementation is `ItemInHandRendererMixin`.

#### VERIFICATION

All twenty responsibility types are present in `content/player/animation/firstPerson`; the 26.2 hand targets compile, are registered in `pulselib.mixins.json`, and the full Gradle build is run for this audit.

### Model-scoped resources and textures

#### SOURCE FEATURE

Replace `PTextureCache` with `PResourceCache` and `PModelResource`; load registered models only and resolve texture references per model.

#### 26.2 IMPLEMENTATION

`PulseLibEvents.RegisterResourceEvent` records model and material-reference mappings. `RuntimeLoader` builds the atlas from those resources, `PModelCache` loads/bakes registered resources only, and `PResourceCache.resolve(model, reference)` performs the lookup in the owning model resource.

#### FILES/SYMBOLS

`PResourceCache`, `PModelResource`, `PTextureReference`, `RuntimeLoader`, `PModelCache`, and `PulseLibEvents.RegisterResourceEvent`.

#### VERIFICATION

`PResourceCache.resolve` keys texture references by `PModelResource`; equal local names in two model resources do not share a registration. `PModelCache` throws `Registered model was not loaded; tried: ...` when no registered candidate was loaded.

### glTF GLB/GLTF fallback

#### SOURCE FEATURE

Allow a registered glTF model to fall back between `.glb` and `.gltf`, preferring `.glb` for an extension-less id and reporting both candidates on failure.

#### 26.2 IMPLEMENTATION

`PGltfModelLoader.modelResourceCandidates` returns the preferred extension followed by the alternate extension. `PModelCache` validates the candidates after loading and includes them in its failure message.

#### FILES/SYMBOLS

`PGltfModelLoader#modelResourceCandidates`, `PModelLoader#modelResourceCandidates`, and `PModelCache#loadModels`.

#### VERIFICATION

The implementation lists `.glb` then `.gltf` for extension-less ids, reverses preference for an explicit extension, and passes the complete candidate list to the registered-model validation error.
