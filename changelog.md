* fixed glTF meshes without a parent bone being discarded by creating a local render bone for them
* fixed glTF animation rotations being applied outside their local bone space
* added automatic opaque, cutout, and translucent texture classification
* added weighted blended order-independent transparency for queued translucent meshes
* fixed mesh render resolvers losing inherited texture, emissive, alpha-mode, and deformation overrides
* fixed withEmissive(false) retaining full-bright lighting from emissive texture metadata
* finally fixed OIT, also added fix to first person renderer
* fixed huge fps drop with active OIT
* fixed Gecko item models using a GeckoLib-compatible item transform instead of the glTF transform
* changed access type for direction setup methods in block renderer
* removed the obsolete `PItemAnimatable` interface; animated items now implement `PAnimatable` directly
* moved animation channel, animation event, and mesh deformer types to PulseLib's internal registries
* added a type-registration event for external animation channels, animation events, and mesh deformers
* rewrite whole first person animations
* replaced `PTextureCache` with `PResourceCache` and `PModelResource`; only registered models are loaded, and material texture references resolve per model
* glTF model-resource registration now falls back between `.glb` and `.gltf`, reporting both checked paths when neither file exists
