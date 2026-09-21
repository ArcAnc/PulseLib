# Port audit: 1976c64 source delta

- Source commit: `1976c6423786e38caa9d96c3f385bd469e887e49`
- Source parent: `45d5282`
- Scope: every record from `git diff --name-status -M SOURCE^ SOURCE`; port only that delta.
- Initial status: all records are `PENDING`.

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
- Port status: `ADAPTED_26_2`
- Notes: 26.1 source behavior: delegates glTF animation sidecars to the shared parser. 26.2 API difference: the current target `PAnimation` has no visibility-track state or constructor yet. 26.2 implementation: delegates event parsing to `PAnimationSidecarParser`; visibility tracks remain for the ANIMATION_CORE port.
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
- Port status: `PORTED`
- Notes: Deleted in the source delta; assess the equivalent removal or replacement on 26.2.
- Target path if renamed: `—`

### RENDER_INFRASTRUCTURE (35)

#### `src/main/java/com/arcanc/pulselib/content/model/baked/AtlasBufferBuilder.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PBakedBone.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PBakedMesh.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderContext.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderMaterial.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderResolver.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PMeshTextureVariants.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/baked/PSubdividedMeshCache.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PBlockRenderer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderLayer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PMeshRenderResolver.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/PRenderer.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/base/PBlockRenderState.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/base/PEntityRenderState.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/base/PItemRenderState.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/base/PRenderState.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlFrameArena.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlGeometryArena.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlIndirectStream.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlInstanceStream.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlMultiDrawExecutor.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlWeightedBlendedOit.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultBlockModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityLayerModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultItemModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/plan/PDrawGroup.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/plan/PFrameCompiler.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/renderer/plan/PRenderPlan.java`

- Source status: `M`
- Subsystem: `RENDER_INFRASTRUCTURE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

### ANIMATION_CORE (56)

#### `src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/ControllerState.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationCameraShake.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventCallbacks.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventDispatcher.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PAnimationManager.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/PLibAnimationTicker.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/instance/InstanceAnimationManager.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/BoneFrame.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannel.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannelType.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationDecodeContext.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvaluationContext.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvent.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventContext.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventType.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventTypes.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationFormatDecoder.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraph.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraphRuntime.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationParameters.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationPoseResolver.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationRuntime.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationSample.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationState.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTrack.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTransition.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationType.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationValue.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationVisibilityTrack.java`

- Source status: `A`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PBlendMode.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PBoneAnimation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PCompiledAnimation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PCondition.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PEventSide.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolationType.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PInterruptionPolicy.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PKeyframe.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PModelPose.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPose.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPoseBlendMode.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPoseEasing.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPoseMixer.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PPoseWriter.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PRawAnimation.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionDelta.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionRuntime.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PTransform.java`

- Source status: `A`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PTransitionInterruptionPolicy.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/animation/PVectorConversion.java`

- Source status: `M`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/PAnimationSidecarParser.java`

- Source status: `A`
- Subsystem: `ANIMATION_CORE`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

### PLAYER_ANIMATION (18)

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchor.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchorPose.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchors.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationBlendMode.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDefinition.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformer.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerApplication.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerContext.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerValueSource.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationFrame.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationHandle.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationInstance.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationMask.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationSpace.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationWeight.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimations.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerBonePose.java`

- Source status: `A`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerPart.java`

- Source status: `M`
- Subsystem: `PLAYER_ANIMATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

### FIRST_PERSON (21)

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmRig.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraMode.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraSpace.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemRig.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPoseStack.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPresentation.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContext.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContexts.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderMode.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderPresentation.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRestPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonTransformMode.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonAnchorPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonMeshAttachmentPose.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonRenderer.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonSettings.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonArmResolver.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonItemResolver.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/package-info.java`

- Source status: `A`
- Subsystem: `FIRST_PERSON`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

### MIXIN_INTEGRATION (18)

#### `src/main/java/com/arcanc/pulselib/content/mixin/BlockEntityRenderStateAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/CameraMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/CubeDefinitionMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/EntityModelSetMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/GameRendererMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/GlBufferAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/HumanoidArmorLayerMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererAccessor.java`

- Source status: `D`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Deleted in the source delta; assess the equivalent removal or replacement on 26.2.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ItemModelResolverAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ItemStackRenderStateAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/LivingEntityRendererMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/ModelPartCubesMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/PlayerRendererMixin.java`

- Source status: `D`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Deleted in the source delta; assess the equivalent removal or replacement on 26.2.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/PlayerRootTransformMixin.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperAccessor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperRenderStateExtractor.java`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/resources/pulselib.mixins.json`

- Source status: `M`
- Subsystem: `MIXIN_INTEGRATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

### ATTACHMENTS_AND_DEFORMERS (56)

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PChannelReference.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerFrame.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerInstance.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerPrepareContext.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerStack.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerValueSource.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformation.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshTessellator.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PPreparedDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PDeformerStream.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerBuffers.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerStack.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentContext.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentLayer.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentRenderer.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachments.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimationMeshAttachmentPose.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAutomaticMeshAttachments.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/animation/attachment/package-info.java`

- Source status: `A`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformableCubeBakeScope.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformedCuboid.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PModelPartCubes.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerDeformerValueSource.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerMeshDeformers.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerVertexDeformer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchor.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentBinding.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentDefinition.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentLayer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSource.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSources.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachments.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolver.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolvers.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/PTransform.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAttachmentLayer.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java`

- Source status: `M`
- Subsystem: `ATTACHMENTS_AND_DEFORMERS`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

### DEMO_TEST_RESOURCES (20)

#### `src/main/java/com/arcanc/pulselib/content/registration/PLibRegistration.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/PRegistry.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/block/TestBlock.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/TestBlockEntity.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/TestBlockEntityRenderer.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/renderState/TestBlockEntityRenderState.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/entity/TestEntity.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/PTestArmor.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/TestEntityRender.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/item/TestArmorItem.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/item/TestBlockItem.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/item/renderer/TestBlockItemRenderer.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/item/renderer/renderState/TestBlockItemRenderState.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerAcrobaticDemo.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerBallDemo.java`

- Source status: `A`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/registration/renderer/TestDayTimeColor.java`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/resources/assets/pulselib/glmodels.zip`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/resources/assets/pulselib/template/player/player_model_template.bbmodel`

- Source status: `A`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/resources/assets/pulselib/template/player/player_model_template.gltf`

- Source status: `A`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Added in the source delta; port this addition against the 26.2 API.
- Target path if renamed: `—`

#### `src/main/resources/assets/pulselib/textures.zip`

- Source status: `M`
- Subsystem: `DEMO_TEST_RESOURCES`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

### DOCUMENTATION (12)

#### `changelog.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/api-reference.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/armor-and-attachments.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/basic.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/installation.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/model-loaders.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/modeldata.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/player-animations.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/pulselib-items.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/render-types-and-queue.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/renderers.md`

- Source status: `M`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `wiki/textures-and-emissive.md`

- Source status: `R`
- Subsystem: `DOCUMENTATION`
- Port status: `PENDING`
- Notes: Renamed in the source delta; port the delta and retain the recorded destination.
- Target path if renamed: `wiki/resources.md`

### BUILD_METADATA (1)

#### `gradle.properties`

- Source status: `M`
- Subsystem: `BUILD_METADATA`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

### OTHER (19)

#### `src/main/java/com/arcanc/pulselib/content/event/ClientEvents.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/event/CommonEvents.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/MolangParser.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PExpressionDependency.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PExpressionEvaluator.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoAnimationEventParser.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoChannelDecoder.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoDecodeContext.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelParser.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PMolangEulerRotationValue.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/data/gecko/PMolangVectorValue.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/PLibDatabase.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/PRenderTypes.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/helpers/PLibCodecs.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/helpers/PLibParserHelper.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
- Target path if renamed: `—`

#### `src/main/java/com/arcanc/pulselib/util/helpers/PLibRenderHelper.java`

- Source status: `M`
- Subsystem: `OTHER`
- Port status: `PENDING`
- Notes: Modified in the source delta; port only the change relative to the source parent.
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
