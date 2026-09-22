# PulseLib 1.21.1 backport audit

Source commit: 1976c6423786e38caa9d96c3f385bd469e887e49

This inventory records the 277 entries reported by git diff --name-status -M SOURCE^ SOURCE. Each entry is PENDING until its owning subsystem is completed. Target paths are planned locations, not production changes.

## 001 — changelog.md

Source path: changelog.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: changelog.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 002 — gradle.properties

Source path: gradle.properties
Source change: M
Subsystem: BUILD_METADATA
Target path: gradle.properties
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 003 — src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 004 — src/main/java/com/arcanc/pulselib/content/animatable/ControllerState.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/ControllerState.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/ControllerState.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 005 — src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 006 — src/main/java/com/arcanc/pulselib/content/animatable/PAnimationCameraShake.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationCameraShake.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationCameraShake.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 007 — src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 008 — src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventCallbacks.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventCallbacks.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventCallbacks.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 009 — src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventDispatcher.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventDispatcher.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationEventDispatcher.java
Status: PORTED
Notes: Ported from SOURCE^ -> SOURCE; preserved the source runtime behavior.

## 010 — src/main/java/com/arcanc/pulselib/content/animatable/PAnimationManager.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationManager.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/PAnimationManager.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 011 — src/main/java/com/arcanc/pulselib/content/animatable/PLibAnimationTicker.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/PLibAnimationTicker.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/PLibAnimationTicker.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 012 — src/main/java/com/arcanc/pulselib/content/animatable/instance/InstanceAnimationManager.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/instance/InstanceAnimationManager.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/instance/InstanceAnimationManager.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 013 — src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java

Source path: src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 014 — src/main/java/com/arcanc/pulselib/content/event/ClientEvents.java

Source path: src/main/java/com/arcanc/pulselib/content/event/ClientEvents.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/event/ClientEvents.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 015 — src/main/java/com/arcanc/pulselib/content/event/CommonEvents.java

Source path: src/main/java/com/arcanc/pulselib/content/event/CommonEvents.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/event/CommonEvents.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 016 — src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java

Source path: src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 017 — src/main/java/com/arcanc/pulselib/content/mixin/BlockEntityRenderStateAccessor.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/BlockEntityRenderStateAccessor.java
Source change: M
Subsystem: VANILLA_1_21_1_INTEGRATION
Target path: src/main/java/com/arcanc/pulselib/content/mixin/BlockEntityRenderStateAccessor.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain block state and render inputs through modern block-entity extraction. API difference: BlockEntityRenderState does not exist in 1.21.1. 1.21.1 implementation: read the live BlockEntity and render arguments in the native renderer lifecycle. target symbol: PBlockRenderer.render(T, float, PoseStack, MultiBufferSource, int, int).
## 018 — src/main/java/com/arcanc/pulselib/content/mixin/CameraMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/CameraMixin.java
Source change: M
Subsystem: VANILLA_1_21_1_INTEGRATION
Target path: src/main/java/com/arcanc/pulselib/content/mixin/CameraMixin.java
Status: ADAPTED_1_21_1
Notes: source behavior: apply animated camera pose and shake after vanilla camera setup. API difference: 1.21.1 has Camera.setup rather than the modern camera render-state path. 1.21.1 implementation: CameraMixin injects at Camera.setup tail and updates the live camera position and rotation. target symbol: CameraMixin.pulselib$followAnimatedHead.
## 019 — src/main/java/com/arcanc/pulselib/content/mixin/CubeDefinitionMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/CubeDefinitionMixin.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/mixin/CubeDefinitionMixin.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 020 — src/main/java/com/arcanc/pulselib/content/mixin/EntityModelSetMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/EntityModelSetMixin.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/mixin/EntityModelSetMixin.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 021 — src/main/java/com/arcanc/pulselib/content/mixin/GameRendererMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/GameRendererMixin.java
Source change: M
Subsystem: VANILLA_1_21_1_INTEGRATION
Target path: src/main/java/com/arcanc/pulselib/content/mixin/GameRendererMixin.java
Status: ADAPTED_1_21_1
Notes: source behavior: flush first-person PulseLib geometry after vanilla hands and items. API difference: 1.21.1 has no SubmitNodeCollector. 1.21.1 implementation: flush the FIRST_PERSON PRenderQueue stage after ItemInHandRenderer.renderHandsWithItems. target symbol: GameRendererMixin.pulselib$flushFirstPersonItems.
## 022 — src/main/java/com/arcanc/pulselib/content/mixin/GlBufferAccessor.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/GlBufferAccessor.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/mixin/GlBufferAccessor.java
Status: ADAPTED_1_21_1
Notes: source behavior: reach renderer buffer storage for GPU submissions. API difference: the source accessor targets newer rendering internals unavailable in 1.21.1. 1.21.1 implementation: the legacy GL backend owns geometry and instance buffers directly. target symbol: GlResourceRegistry and McLegacyGlHostBridge.
## 023 — src/main/java/com/arcanc/pulselib/content/mixin/HumanoidArmorLayerMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/HumanoidArmorLayerMixin.java
Source change: M
Subsystem: ATTACHMENTS_ARMOR
Target path: src/main/java/com/arcanc/pulselib/content/mixin/HumanoidArmorLayerMixin.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 024 — src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererAccessor.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererAccessor.java
Source change: D
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererAccessor.java
Status: ADAPTED_1_21_1
Notes: source behavior: obsolete private arm bridge removed by the newer renderer. API difference: 1.21.1 has private ItemInHandRenderer orchestration but direct PlayerRenderer hand calls. 1.21.1 implementation: ItemInHandRendererMixin wraps those direct calls, so no accessor is registered. target symbol: ItemInHandRendererMixin.
## 025 — src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererMixin.java
Source change: M
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/mixin/ItemInHandRendererMixin.java
Status: ADAPTED_1_21_1
Notes: source behavior: scope first-person rendering and replace final arm/item submissions. API difference: 1.21.1 uses MultiBufferSource and direct ItemInHandRenderer calls. 1.21.1 implementation: wrap renderHandsWithItems, renderArmWithItem, renderPlayerArm, and renderItem; retain vanilla equip/swing/use transforms. target symbol: ItemInHandRendererMixin.
## 026 — src/main/java/com/arcanc/pulselib/content/mixin/ItemModelResolverAccessor.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/ItemModelResolverAccessor.java
Source change: M
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/mixin/ItemModelResolverAccessor.java
Status: ADAPTED_1_21_1
Notes: source behavior: supply the first-person renderer integration boundary. API difference: modern item render states and wrappers are absent in 1.21.1. 1.21.1 implementation: retain the direct ItemInHandRenderer/BEWLR hooks for the following integration stage. target symbol: PFirstPersonRenderContexts and PPlayerFirstPersonRenderer.
## 027 — src/main/java/com/arcanc/pulselib/content/mixin/ItemStackRenderStateAccessor.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/ItemStackRenderStateAccessor.java
Source change: M
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/mixin/ItemStackRenderStateAccessor.java
Status: ADAPTED_1_21_1
Notes: source behavior: supply the first-person renderer integration boundary. API difference: modern item render states and wrappers are absent in 1.21.1. 1.21.1 implementation: retain the direct ItemInHandRenderer/BEWLR hooks for the following integration stage. target symbol: PFirstPersonRenderContexts and PPlayerFirstPersonRenderer.
## 028 — src/main/java/com/arcanc/pulselib/content/mixin/LivingEntityRendererMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/LivingEntityRendererMixin.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/mixin/LivingEntityRendererMixin.java
Status: ADAPTED_1_21_1
Notes: source behavior: apply and restore the third-person PPlayerAnimationFrame pose and root transform. API difference: 1.21.1 renders live entities instead of modern render states. 1.21.1 implementation: inject around EntityModel.setupAnim in LivingEntityRenderer.render. target symbol: LivingEntityRendererMixin.
## 029 — src/main/java/com/arcanc/pulselib/content/mixin/ModelPartCubesMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/ModelPartCubesMixin.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/mixin/ModelPartCubesMixin.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 030 — src/main/java/com/arcanc/pulselib/content/mixin/PlayerRendererMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/PlayerRendererMixin.java
Source change: D
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/mixin/PlayerRendererMixin.java
Status: ADAPTED_1_21_1
Notes: source behavior: remove the old first-person PlayerRenderer bridge. API difference: 1.21.1 still exposes direct PlayerRenderer hand calls from ItemInHandRenderer. 1.21.1 implementation: ItemInHandRendererMixin owns those calls; PlayerRendererMixin is removed from mixin config to avoid duplicate poses. target symbol: ItemInHandRendererMixin.
## 031 — src/main/java/com/arcanc/pulselib/content/mixin/PlayerRootTransformMixin.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/PlayerRootTransformMixin.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/mixin/PlayerRootTransformMixin.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain the player animation, frame, attachment, or armor responsibility. API difference: source relies in places on modern render states or player hooks. 1.21.1 implementation: use direct PlayerModel/LivingEntityRenderer/PlayerRenderer callbacks and native NeoForge client extensions. target symbol: PPlayerAnimations, PPlayerAnimationFrame, PPlayerAnimatedAttachmentLayer, or PLibArmorHandler.
## 032 — src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperAccessor.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperAccessor.java
Source change: M
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperAccessor.java
Status: ADAPTED_1_21_1
Notes: source behavior: supply the first-person renderer integration boundary. API difference: modern item render states and wrappers are absent in 1.21.1. 1.21.1 implementation: retain the direct ItemInHandRenderer/BEWLR hooks for the following integration stage. target symbol: PFirstPersonRenderContexts and PPlayerFirstPersonRenderer.
## 033 — src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperRenderStateExtractor.java

Source path: src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperRenderStateExtractor.java
Source change: M
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/mixin/SpecialModelWrapperRenderStateExtractor.java
Status: ADAPTED_1_21_1
Notes: source behavior: supply the first-person renderer integration boundary. API difference: modern item render states and wrappers are absent in 1.21.1. 1.21.1 implementation: retain the direct ItemInHandRenderer/BEWLR hooks for the following integration stage. target symbol: PFirstPersonRenderContexts and PPlayerFirstPersonRenderer.
## 034 — src/main/java/com/arcanc/pulselib/content/model/PBone.java

Source path: src/main/java/com/arcanc/pulselib/content/model/PBone.java
Source change: M
Subsystem: OTHER
Target path: src/main/java/com/arcanc/pulselib/content/model/PBone.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 035 — src/main/java/com/arcanc/pulselib/content/model/PMaterial.java

Source path: src/main/java/com/arcanc/pulselib/content/model/PMaterial.java
Source change: A
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/PMaterial.java
Status: PORTED
Notes: New source API; port after its owning subsystem is scheduled.

## 036 — src/main/java/com/arcanc/pulselib/content/model/PMesh.java

Source path: src/main/java/com/arcanc/pulselib/content/model/PMesh.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/PMesh.java
Status: PORTED
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 037 — src/main/java/com/arcanc/pulselib/content/model/PMeshPrimitive.java

Source path: src/main/java/com/arcanc/pulselib/content/model/PMeshPrimitive.java
Source change: A
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/PMeshPrimitive.java
Status: PORTED
Notes: New source API; port after its owning subsystem is scheduled.

## 038 — src/main/java/com/arcanc/pulselib/content/model/PModel.java

Source path: src/main/java/com/arcanc/pulselib/content/model/PModel.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/PModel.java
Status: PORTED
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 039 — src/main/java/com/arcanc/pulselib/content/model/PTextureReference.java

Source path: src/main/java/com/arcanc/pulselib/content/model/PTextureReference.java
Source change: A
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/PTextureReference.java
Status: PORTED
Notes: New source API; port after its owning subsystem is scheduled.

## 040 — src/main/java/com/arcanc/pulselib/content/model/animation/BoneFrame.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/BoneFrame.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/BoneFrame.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 041 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimation.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimation.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimation.java
Status: PORTED
Notes: Ported from SOURCE^ -> SOURCE; preserved the source runtime behavior.

## 042 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannel.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannel.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannel.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 043 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannelType.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannelType.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationChannelType.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 044 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationDecodeContext.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationDecodeContext.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationDecodeContext.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 045 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvaluationContext.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvaluationContext.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvaluationContext.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 046 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvent.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvent.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEvent.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 047 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventContext.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventContext.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventContext.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 048 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventType.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventType.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventType.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 049 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventTypes.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventTypes.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationEventTypes.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 050 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationFormatDecoder.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationFormatDecoder.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationFormatDecoder.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 051 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraph.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraph.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraph.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 052 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraphRuntime.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraphRuntime.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraphRuntime.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 053 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationParameters.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationParameters.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationParameters.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 054 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationPoseResolver.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationPoseResolver.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationPoseResolver.java
Status: ADAPTED_1_21_1
Notes: source behavior: resolved canonical TRS model transforms, relative transforms, and animation visibility; API difference: JSpecify annotations are unavailable; 1.21.1 implementation: uses org.jetbrains Nullable; target symbol: PAnimationPoseResolver.

## 055 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationRuntime.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationRuntime.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationRuntime.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 056 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationSample.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationSample.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationSample.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 057 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationState.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationState.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationState.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 058 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTrack.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTrack.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTrack.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 059 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTransition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTransition.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationTransition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 060 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationType.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationType.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationType.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 061 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationValue.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationValue.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationValue.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 062 — src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationVisibilityTrack.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationVisibilityTrack.java
Source change: A
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationVisibilityTrack.java
Status: PORTED
Notes: Ported from SOURCE^ -> SOURCE; preserved the source runtime behavior.

## 063 — src/main/java/com/arcanc/pulselib/content/model/animation/PBlendMode.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PBlendMode.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PBlendMode.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 064 — src/main/java/com/arcanc/pulselib/content/model/animation/PBoneAnimation.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PBoneAnimation.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PBoneAnimation.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 065 — src/main/java/com/arcanc/pulselib/content/model/animation/PCompiledAnimation.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PCompiledAnimation.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PCompiledAnimation.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 066 — src/main/java/com/arcanc/pulselib/content/model/animation/PCondition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PCondition.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PCondition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 067 — src/main/java/com/arcanc/pulselib/content/model/animation/PEventSide.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PEventSide.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PEventSide.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 068 — src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolation.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolation.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolation.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 069 — src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolationType.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolationType.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PInterpolationType.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 070 — src/main/java/com/arcanc/pulselib/content/model/animation/PInterruptionPolicy.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PInterruptionPolicy.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PInterruptionPolicy.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 071 — src/main/java/com/arcanc/pulselib/content/model/animation/PKeyframe.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PKeyframe.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PKeyframe.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 072 — src/main/java/com/arcanc/pulselib/content/model/animation/PModelPose.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PModelPose.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PModelPose.java
Status: PORTED
Notes: Ported from SOURCE^ -> SOURCE; preserved the source runtime behavior.

## 073 — src/main/java/com/arcanc/pulselib/content/model/animation/PPose.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PPose.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PPose.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 074 — src/main/java/com/arcanc/pulselib/content/model/animation/PPoseBlendMode.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PPoseBlendMode.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PPoseBlendMode.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 075 — src/main/java/com/arcanc/pulselib/content/model/animation/PPoseEasing.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PPoseEasing.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PPoseEasing.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 076 — src/main/java/com/arcanc/pulselib/content/model/animation/PPoseMixer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PPoseMixer.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PPoseMixer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 077 — src/main/java/com/arcanc/pulselib/content/model/animation/PPoseWriter.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PPoseWriter.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PPoseWriter.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 078 — src/main/java/com/arcanc/pulselib/content/model/animation/PRawAnimation.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PRawAnimation.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PRawAnimation.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 079 — src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionDelta.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionDelta.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionDelta.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 080 — src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionRuntime.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionRuntime.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PRootMotionRuntime.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 081 — src/main/java/com/arcanc/pulselib/content/model/animation/PTransform.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PTransform.java
Source change: A
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PTransform.java
Status: PORTED
Notes: Ported from SOURCE^ -> SOURCE; preserved the source runtime behavior.

## 082 — src/main/java/com/arcanc/pulselib/content/model/animation/PTransitionInterruptionPolicy.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PTransitionInterruptionPolicy.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PTransitionInterruptionPolicy.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 083 — src/main/java/com/arcanc/pulselib/content/model/animation/PVectorConversion.java

Source path: src/main/java/com/arcanc/pulselib/content/model/animation/PVectorConversion.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/content/model/animation/PVectorConversion.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 084 — src/main/java/com/arcanc/pulselib/content/model/baked/AtlasBufferBuilder.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/AtlasBufferBuilder.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/AtlasBufferBuilder.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 085 — src/main/java/com/arcanc/pulselib/content/model/baked/PBakedBone.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/PBakedBone.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/PBakedBone.java
Status: ADAPTED_1_21_1
Notes: source behavior: resolve one animated pose for a bone subtree and suppress invisible bones; API difference: source uses the modern GPU render pass API; 1.21.1 implementation: shares PAnimationPoseResolver with the existing immediate-draw backend; target symbol: PBakedBone.instantDraw(PoseStack, PAnimationPoseResolver, PMeshRenderResolver, PMeshRenderContext).

## 086 — src/main/java/com/arcanc/pulselib/content/model/baked/PBakedMesh.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/PBakedMesh.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/PBakedMesh.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 087 — src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/PBakedModel.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 088 — src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderContext.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderContext.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderContext.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 089 — src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderMaterial.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderMaterial.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderMaterial.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 090 — src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderResolver.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderResolver.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/PMeshRenderResolver.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.



## 091 — src/main/java/com/arcanc/pulselib/content/model/baked/PMeshTextureVariants.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/PMeshTextureVariants.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/PMeshTextureVariants.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 092 — src/main/java/com/arcanc/pulselib/content/model/baked/PSubdividedMeshCache.java

Source path: src/main/java/com/arcanc/pulselib/content/model/baked/PSubdividedMeshCache.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/baked/PSubdividedMeshCache.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 093 — src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDefinition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDefinition.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDefinition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 094 — src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PBendDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 095 — src/main/java/com/arcanc/pulselib/content/model/deformer/PChannelReference.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PChannelReference.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PChannelReference.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 096 — src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerFrame.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerFrame.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerFrame.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 097 — src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerInstance.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerInstance.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerInstance.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 098 — src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerPrepareContext.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerPrepareContext.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerPrepareContext.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 099 — src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerStack.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerStack.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerStack.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 100 — src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerValueSource.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerValueSource.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PDeformerValueSource.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 101 — src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDefinition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDefinition.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDefinition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 102 — src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PHingeDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 103 — src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformation.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformation.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformation.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 104 — src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 105 — src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshTessellator.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshTessellator.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PMeshTessellator.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 106 — src/main/java/com/arcanc/pulselib/content/model/deformer/PPreparedDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PPreparedDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PPreparedDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 107 — src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDefinition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDefinition.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDefinition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 108 — src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PSquashDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 109 — src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDefinition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDefinition.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDefinition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 110 — src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PStretchDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 111 — src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDefinition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDefinition.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDefinition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 112 — src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PTaperDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 113 — src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDefinition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDefinition.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDefinition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 114 — src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PTwistDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 115 — src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDefinition.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDefinition.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDefinition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 116 — src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/PWaveDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 117 — src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PDeformerStream.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PDeformerStream.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PDeformerStream.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 118 — src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerBuffers.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerBuffers.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerBuffers.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 119 — src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerStack.java

Source path: src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerStack.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/model/deformer/gpu/PGpuDeformerStack.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 120 — src/main/java/com/arcanc/pulselib/content/model/resource/PModelResource.java

Source path: src/main/java/com/arcanc/pulselib/content/model/resource/PModelResource.java
Source change: A
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/resource/PModelResource.java
Status: PORTED
Notes: New source API; port after its owning subsystem is scheduled.

## 121 — src/main/java/com/arcanc/pulselib/content/model/resource/package-info.java

Source path: src/main/java/com/arcanc/pulselib/content/model/resource/package-info.java
Source change: A
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/resource/package-info.java
Status: PORTED
Notes: New source API; port after its owning subsystem is scheduled.

## 122 — src/main/java/com/arcanc/pulselib/content/model/textures/PAlphaMode.java

Source path: src/main/java/com/arcanc/pulselib/content/model/textures/PAlphaMode.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/textures/PAlphaMode.java
Status: ALREADY_PRESENT
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 123 — src/main/java/com/arcanc/pulselib/content/model/textures/PTextureAlphaClassifier.java

Source path: src/main/java/com/arcanc/pulselib/content/model/textures/PTextureAlphaClassifier.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/textures/PTextureAlphaClassifier.java
Status: ALREADY_PRESENT
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 124 — src/main/java/com/arcanc/pulselib/content/model/textures/atlas/PLibSpriteMetadata.java

Source path: src/main/java/com/arcanc/pulselib/content/model/textures/atlas/PLibSpriteMetadata.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/textures/atlas/PLibSpriteMetadata.java
Status: ALREADY_PRESENT
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 125 — src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java

Source path: src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/content/model/textures/atlas/RuntimeLoader.java
Status: ADAPTED_1_21_1
Notes: source behavior: rebuild the atlas from textures registered under each model resource. API difference: 1.21.1 SpriteSource uses SpriteResourceLoader and RegisterMaterialAtlasesEvent. 1.21.1 implementation: collect registered textures, namespace atlas sprites with PResourceCache.spriteId, and load through the target SpriteResourceLoader. target symbol: RuntimeLoader.run(ResourceManager, Output).

## 126 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchor.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchor.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchor.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 127 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchorPose.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchorPose.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchorPose.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 128 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchors.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchors.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationAnchors.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 129 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationBlendMode.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationBlendMode.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationBlendMode.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 130 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDefinition.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDefinition.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDefinition.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain the player animation, frame, attachment, or armor responsibility. API difference: source relies in places on modern render states or player hooks. 1.21.1 implementation: use direct PlayerModel/LivingEntityRenderer/PlayerRenderer callbacks and native NeoForge client extensions. target symbol: PPlayerAnimations, PPlayerAnimationFrame, PPlayerAnimatedAttachmentLayer, or PLibArmorHandler.
## 131 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformer.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 132 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerApplication.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerApplication.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerApplication.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 133 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerContext.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerContext.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerContext.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 134 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerValueSource.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerValueSource.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationDeformerValueSource.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 135 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationFrame.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationFrame.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationFrame.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 136 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationHandle.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationHandle.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationHandle.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 137 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationInstance.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationInstance.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationInstance.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain the player animation, frame, attachment, or armor responsibility. API difference: source relies in places on modern render states or player hooks. 1.21.1 implementation: use direct PlayerModel/LivingEntityRenderer/PlayerRenderer callbacks and native NeoForge client extensions. target symbol: PPlayerAnimations, PPlayerAnimationFrame, PPlayerAnimatedAttachmentLayer, or PLibArmorHandler.
## 138 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationMask.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationMask.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationMask.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 139 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationSpace.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationSpace.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationSpace.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 140 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationWeight.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationWeight.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimationWeight.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 141 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimations.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimations.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerAnimations.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain the player animation, frame, attachment, or armor responsibility. API difference: source relies in places on modern render states or player hooks. 1.21.1 implementation: use direct PlayerModel/LivingEntityRenderer/PlayerRenderer callbacks and native NeoForge client extensions. target symbol: PPlayerAnimations, PPlayerAnimationFrame, PPlayerAnimatedAttachmentLayer, or PLibArmorHandler.
## 142 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerBonePose.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerBonePose.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerBonePose.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 143 — src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerPart.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerPart.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/PPlayerPart.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain the player animation, frame, attachment, or armor responsibility. API difference: source relies in places on modern render states or player hooks. 1.21.1 implementation: use direct PlayerModel/LivingEntityRenderer/PlayerRenderer callbacks and native NeoForge client extensions. target symbol: PPlayerAnimations, PPlayerAnimationFrame, PPlayerAnimatedAttachmentLayer, or PLibArmorHandler.
## 144 — src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentContext.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentContext.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentContext.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 145 — src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentLayer.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentLayer.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentLayer.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 146 — src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentRenderer.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentRenderer.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachmentRenderer.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 147 — src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachments.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachments.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimatedAttachments.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 148 — src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimationMeshAttachmentPose.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimationMeshAttachmentPose.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAnimationMeshAttachmentPose.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 149 — src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAutomaticMeshAttachments.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAutomaticMeshAttachments.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/PPlayerAutomaticMeshAttachments.java
Status: PORTED
Notes: PORTED: source library type is available on 1.21.1 and is wired to the shared PPlayerAnimationFrame runtime.
## 150 — src/main/java/com/arcanc/pulselib/content/player/animation/attachment/package-info.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/package-info.java
Source change: A
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/attachment/package-info.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 151 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmPose.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmPose.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmPose.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 152 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmRig.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmRig.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonArmRig.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 153 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraMode.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraMode.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraMode.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 154 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraSpace.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraSpace.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonCameraSpace.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 155 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemPose.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemPose.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemPose.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 156 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemRig.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemRig.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonItemRig.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 157 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPoseStack.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPoseStack.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPoseStack.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 158 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPresentation.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPresentation.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonPresentation.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 159 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContext.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContext.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContext.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 160 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContexts.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContexts.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderContexts.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 161 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderMode.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderMode.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderMode.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 162 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderPresentation.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderPresentation.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRenderPresentation.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 163 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRestPose.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRestPose.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonRestPose.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 164 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonTransformMode.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonTransformMode.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PFirstPersonTransformMode.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 165 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonAnchorPose.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonAnchorPose.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonAnchorPose.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 166 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonMeshAttachmentPose.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonMeshAttachmentPose.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonMeshAttachmentPose.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 167 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonRenderer.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonRenderer.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonRenderer.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 168 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonSettings.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonSettings.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PPlayerFirstPersonSettings.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 169 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonArmResolver.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonArmResolver.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonArmResolver.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 170 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonItemResolver.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonItemResolver.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/PVanillaFirstPersonItemResolver.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 171 — src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/package-info.java

Source path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/package-info.java
Source change: A
Subsystem: FIRST_PERSON_CORE
Target path: src/main/java/com/arcanc/pulselib/content/player/animation/firstPerson/package-info.java
Status: PORTED
Notes: PORTED: version-independent first-person API is available on 1.21.1; PVanillaFirstPersonArmResolver and PVanillaFirstPersonItemResolver form the explicit native renderer boundary.
## 172 — src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformableCubeBakeScope.java

Source path: src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformableCubeBakeScope.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformableCubeBakeScope.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 173 — src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformedCuboid.java

Source path: src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformedCuboid.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/player/deformer/PDeformedCuboid.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 174 — src/main/java/com/arcanc/pulselib/content/player/deformer/PModelPartCubes.java

Source path: src/main/java/com/arcanc/pulselib/content/player/deformer/PModelPartCubes.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/player/deformer/PModelPartCubes.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 175 — src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerDeformerValueSource.java

Source path: src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerDeformerValueSource.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerDeformerValueSource.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 176 — src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerMeshDeformers.java

Source path: src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerMeshDeformers.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerMeshDeformers.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 177 — src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerVertexDeformer.java

Source path: src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerVertexDeformer.java
Source change: M
Subsystem: MODEL_BAKING_DEFORMERS
Target path: src/main/java/com/arcanc/pulselib/content/player/deformer/PPlayerVertexDeformer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 178 — src/main/java/com/arcanc/pulselib/content/registration/PLibRegistration.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/PLibRegistration.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/PLibRegistration.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 179 — src/main/java/com/arcanc/pulselib/content/registration/PRegistry.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/PRegistry.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/PRegistry.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 180 — src/main/java/com/arcanc/pulselib/content/registration/block/TestBlock.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/block/TestBlock.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/block/TestBlock.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 181 — src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/TestBlockEntity.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/TestBlockEntity.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/TestBlockEntity.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 182 — src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/TestBlockEntityRenderer.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/TestBlockEntityRenderer.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/TestBlockEntityRenderer.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 183 — src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/renderState/TestBlockEntityRenderState.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/renderState/TestBlockEntityRenderState.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/block/block_entity/ber/renderState/TestBlockEntityRenderState.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 184 — src/main/java/com/arcanc/pulselib/content/registration/entity/TestEntity.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/entity/TestEntity.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/entity/TestEntity.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 185 — src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/PTestArmor.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/PTestArmor.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/PTestArmor.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 186 — src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/TestEntityRender.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/TestEntityRender.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/entity/renderer/TestEntityRender.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 187 — src/main/java/com/arcanc/pulselib/content/registration/item/TestArmorItem.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/item/TestArmorItem.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/item/TestArmorItem.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 188 — src/main/java/com/arcanc/pulselib/content/registration/item/TestBlockItem.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/item/TestBlockItem.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/item/TestBlockItem.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 189 — src/main/java/com/arcanc/pulselib/content/registration/item/renderer/TestBlockItemRenderer.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/item/renderer/TestBlockItemRenderer.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/item/renderer/TestBlockItemRenderer.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 190 — src/main/java/com/arcanc/pulselib/content/registration/item/renderer/renderState/TestBlockItemRenderState.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/item/renderer/renderState/TestBlockItemRenderState.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/item/renderer/renderState/TestBlockItemRenderState.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 191 — src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerAcrobaticDemo.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerAcrobaticDemo.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerAcrobaticDemo.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 192 — src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerBallDemo.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerBallDemo.java
Source change: A
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/player/PPlayerBallDemo.java
Status: PENDING
Notes: New source API; port after its owning subsystem is scheduled.

## 193 — src/main/java/com/arcanc/pulselib/content/registration/renderer/TestDayTimeColor.java

Source path: src/main/java/com/arcanc/pulselib/content/registration/renderer/TestDayTimeColor.java
Source change: M
Subsystem: REGISTRATION_EVENTS
Target path: src/main/java/com/arcanc/pulselib/content/registration/renderer/TestDayTimeColor.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 194 — src/main/java/com/arcanc/pulselib/content/renderer/PBlockRenderer.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/PBlockRenderer.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/PBlockRenderer.java
Status: ADAPTED_1_21_1
Notes: source behavior: render registered model meshes with model-local texture references. API difference: source submits BlockEntityRenderState through SubmitNodeCollector. 1.21.1 implementation: render live block entities through BlockEntityRenderer.render, resolve PMeshRenderMaterial, and queue using PResourceCache.ATLAS_LOCATION. target symbol: PBlockRenderer.render and submitBone.
## 195 — src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderLayer.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderLayer.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderLayer.java
Status: ADAPTED_1_21_1
Notes: source behavior: attach layer meshes to entity-bone transforms while carrying material context. API difference: modern entity render states are absent in 1.21.1. 1.21.1 implementation: extract transforms from the live entity render traversal and submit through PEntityRenderer. target symbol: PEntityRenderLayer.submit and PEntityRenderer.perBoneSubmit.
## 196 — src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java
Status: ADAPTED_1_21_1
Notes: source behavior: evaluate animated entity meshes and resolve textures from each model resource. API difference: source renderer consumes EntityRenderState and SubmitNodeCollector. 1.21.1 implementation: direct EntityRenderer.render evaluates live animation state and queues material output through PResourceCache.ATLAS_LOCATION. target symbol: PEntityRenderer.render and submitBone.
## 197 — src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java
Status: ADAPTED_1_21_1
Notes: source behavior: render special animated items in GUI, first-person, and world display contexts using model-local resources. API difference: ItemModelResolver, ItemStackRenderState, and SpecialModelWrapper are absent in 1.21.1. 1.21.1 implementation: BlockEntityWithoutLevelRenderer.renderByItem receives ItemStack and ItemDisplayContext directly and submits material output through PResourceCache.ATLAS_LOCATION. target symbol: PItemRenderer.renderByItem and trueSubmit.
## 198 — src/main/java/com/arcanc/pulselib/content/renderer/PMeshRenderResolver.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/PMeshRenderResolver.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/PMeshRenderResolver.java
Status: ADAPTED_1_21_1
Notes: source behavior: resolve per-mesh material overrides during state submission. API difference: source resolver carries modern renderer state. 1.21.1 implementation: resolve directly from live animatable, item stack, display context, bone, and mesh. target symbol: content.model.baked.PMeshRenderResolver and PItemRenderer.resolveMeshRender.
## 199 — src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/PRenderQueue.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 200 — src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/PRenderStagesHandler.java
Status: ADAPTED_1_21_1
Notes: source behavior: flush solid, entity, translucent, and OIT work at the correct renderer stages. API difference: 1.21.1 RenderLevelStageEvent has Stage constants rather than source after-feature event subclasses. 1.21.1 implementation: flush at AFTER_BLOCK_ENTITIES and AFTER_PARTICLES, then composite at AFTER_WEATHER. target symbol: PRenderStagesHandler.renderLevelStages.
## 201 — src/main/java/com/arcanc/pulselib/content/renderer/PRenderer.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/PRenderer.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/PRenderer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 202 — src/main/java/com/arcanc/pulselib/content/renderer/base/PBlockRenderState.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/base/PBlockRenderState.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/base/PBlockRenderState.java
Status: ADAPTED_1_21_1
Notes: source behavior: keep block rendering inputs after extraction. API difference: vanilla BlockEntityRenderState is unavailable. 1.21.1 implementation: use the live BlockEntity passed to the native render callback. target symbol: PBlockRenderer.render.
## 203 — src/main/java/com/arcanc/pulselib/content/renderer/base/PEntityRenderState.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/base/PEntityRenderState.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/base/PEntityRenderState.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain entity animation and model inputs after extraction. API difference: vanilla EntityRenderState is unavailable. 1.21.1 implementation: evaluate data during the live EntityRenderer callback. target symbol: PEntityRenderer.render.
## 204 — src/main/java/com/arcanc/pulselib/content/renderer/base/PItemRenderState.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/base/PItemRenderState.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/base/PItemRenderState.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain item stack, display context, lighting, and overlay across item extraction and submission. API difference: ItemStackRenderState is unavailable. 1.21.1 implementation: use renderByItem arguments directly. target symbol: PItemRenderer.renderByItem.
## 205 — src/main/java/com/arcanc/pulselib/content/renderer/base/PRenderState.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/base/PRenderState.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/base/PRenderState.java
Status: ADAPTED_1_21_1
Notes: source behavior: provide a renderer-neutral extracted state contract. API difference: 1.21.1 vanilla rendering is direct rather than extraction-based. 1.21.1 implementation: preserve the responsibility in renderer-local live data and PRenderQueue submissions. target symbol: PBlockRenderer, PEntityRenderer, and PItemRenderer.
## 206 — src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlFrameArena.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlFrameArena.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlFrameArena.java
Status: ADAPTED_1_21_1
Notes: source behavior: fence-backed frame-slot reuse; API difference: modern GL backend is absent; 1.21.1 implementation: legacy GlFrameArena; target symbol: legacy.GlFrameArena.

## 207 — src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlGeometryArena.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlGeometryArena.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlGeometryArena.java
Status: ADAPTED_1_21_1
Notes: source behavior: shared GPU geometry allocation; API difference: modern GL backend is absent; 1.21.1 implementation: legacy GlGeometryArena; target symbol: legacy.GlGeometryArena.

## 208 — src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlIndirectStream.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlIndirectStream.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlIndirectStream.java
Status: ADAPTED_1_21_1
Notes: source behavior: indirect draw command streaming; API difference: modern GL backend is absent; 1.21.1 implementation: legacy indirect buffer in GlDrawExecutor; target symbol: legacy.GlDrawExecutor.

## 209 — src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlInstanceStream.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlInstanceStream.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlInstanceStream.java
Status: ADAPTED_1_21_1
Notes: source behavior: instance data streaming; API difference: modern GL backend is absent; 1.21.1 implementation: legacy instance buffer in GlDrawExecutor; target symbol: legacy.GlDrawExecutor.

## 210 — src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlMultiDrawExecutor.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlMultiDrawExecutor.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlMultiDrawExecutor.java
Status: ADAPTED_1_21_1
Notes: source behavior: multi-draw submission and deformer stream binding; API difference: modern GL backend is absent; 1.21.1 implementation: legacy multi-draw path; target symbol: legacy.GlDrawExecutor.

## 211 — src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlWeightedBlendedOit.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlWeightedBlendedOit.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/gl/PGlWeightedBlendedOit.java
Status: ADAPTED_1_21_1
Notes: source behavior: weighted blended OIT accumulation and composite; API difference: modern GL backend is absent; 1.21.1 implementation: legacy GlWeightedBlendedOit; target symbol: legacy.GlWeightedBlendedOit.

## 212 — src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultBlockModelData.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultBlockModelData.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultBlockModelData.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source removes model-data texture registration; target model data delegates texture ownership to the registered PModelResource and exposes no global texture builder.
## 213 — src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityLayerModelData.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityLayerModelData.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityLayerModelData.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source removes model-data texture registration; target model data delegates texture ownership to the registered PModelResource and exposes no global texture builder.
## 214 — src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityModelData.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityModelData.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultEntityModelData.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source removes model-data texture registration; target model data delegates texture ownership to the registered PModelResource and exposes no global texture builder.
## 215 — src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultItemModelData.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultItemModelData.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/DefaultItemModelData.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source removes model-data texture registration; target model data delegates texture ownership to the registered PModelResource and exposes no global texture builder.
## 216 — src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: PModelData no longer stores or resolves textures; PResourceCache supplies the registered model-local texture context.
## 217 — src/main/java/com/arcanc/pulselib/content/renderer/plan/PDrawGroup.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/plan/PDrawGroup.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/plan/PDrawGroup.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 218 — src/main/java/com/arcanc/pulselib/content/renderer/plan/PFrameCompiler.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/plan/PFrameCompiler.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/plan/PFrameCompiler.java
Status: ADAPTED_1_21_1
Notes: source behavior: batch draw resources by identity instead of structural equality; API difference: 1.21.1 plans include PDrawCommand; 1.21.1 implementation: identity compares pipeline and mesh while retaining command equality; target symbol: PFrameCompiler.DrawKey.

## 219 — src/main/java/com/arcanc/pulselib/content/renderer/plan/PRenderPlan.java

Source path: src/main/java/com/arcanc/pulselib/content/renderer/plan/PRenderPlan.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/content/renderer/plan/PRenderPlan.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the source runtime behavior is already retained by the 1.21.1 resource-model, deformer, or legacy render implementation; remaining source changes are documentation-only.

## 220 — src/main/java/com/arcanc/pulselib/data/PAnimationSidecarParser.java

Source path: src/main/java/com/arcanc/pulselib/data/PAnimationSidecarParser.java
Source change: A
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/PAnimationSidecarParser.java
Status: ADAPTED_1_21_1
Notes: source behavior: merge animation events and visibility sidecars; API difference: 26.x Identifier is absent; 1.21.1 implementation: ResourceLocation parsing and registry lookup; target symbol: PAnimationSidecarParser.

## 221 — src/main/java/com/arcanc/pulselib/data/PModelLoader.java

Source path: src/main/java/com/arcanc/pulselib/data/PModelLoader.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/data/PModelLoader.java
Status: PORTED
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 222 — src/main/java/com/arcanc/pulselib/data/gecko/MolangParser.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/MolangParser.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gecko/MolangParser.java
Status: ADAPTED_1_21_1
Notes: source behavior: full Molang expression evaluation; API difference: JSpecify annotations are unavailable; 1.21.1 implementation: uses org.jetbrains Nullable; target symbol: MolangParser.

## 223 — src/main/java/com/arcanc/pulselib/data/gecko/PExpressionDependency.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PExpressionDependency.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PExpressionDependency.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 224 — src/main/java/com/arcanc/pulselib/data/gecko/PExpressionEvaluator.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PExpressionEvaluator.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PExpressionEvaluator.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 225 — src/main/java/com/arcanc/pulselib/data/gecko/PGeckoAnimationEventParser.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoAnimationEventParser.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoAnimationEventParser.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 226 — src/main/java/com/arcanc/pulselib/data/gecko/PGeckoChannelDecoder.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoChannelDecoder.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoChannelDecoder.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 227 — src/main/java/com/arcanc/pulselib/data/gecko/PGeckoDecodeContext.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoDecodeContext.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoDecodeContext.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 228 — src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelLoader.java
Status: PORTED
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 229 — src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelParser.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelParser.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PGeckoModelParser.java
Status: PORTED
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 230 — src/main/java/com/arcanc/pulselib/data/gecko/PMolangEulerRotationValue.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PMolangEulerRotationValue.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PMolangEulerRotationValue.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 231 — src/main/java/com/arcanc/pulselib/data/gecko/PMolangVectorValue.java

Source path: src/main/java/com/arcanc/pulselib/data/gecko/PMolangVectorValue.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gecko/PMolangVectorValue.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 232 — src/main/java/com/arcanc/pulselib/data/gltf/PGltfAnimationEventSidecarParser.java

Source path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfAnimationEventSidecarParser.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfAnimationEventSidecarParser.java
Status: PORTED
Notes: Ported from SOURCE^ -> SOURCE; preserved the source runtime behavior.

## 233 — src/main/java/com/arcanc/pulselib/data/gltf/PGltfChannelDecoder.java

Source path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfChannelDecoder.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfChannelDecoder.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 234 — src/main/java/com/arcanc/pulselib/data/gltf/PGltfDecodeContext.java

Source path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfDecodeContext.java
Source change: M
Subsystem: ANIMATION_CORE
Target path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfDecodeContext.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: the runtime API was already present in the 1.21.1 baseline; SOURCE^ -> SOURCE changes are documentation or import hygiene.

## 235 — src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java

Source path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelLoader.java
Status: PORTED
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 236 — src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelParser.java

Source path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelParser.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/data/gltf/PGltfModelParser.java
Status: PORTED
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 237 — src/main/java/com/arcanc/pulselib/util/PLibDatabase.java

Source path: src/main/java/com/arcanc/pulselib/util/PLibDatabase.java
Source change: M
Subsystem: OTHER
Target path: src/main/java/com/arcanc/pulselib/util/PLibDatabase.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 238 — src/main/java/com/arcanc/pulselib/util/PModelCache.java

Source path: src/main/java/com/arcanc/pulselib/util/PModelCache.java
Source change: M
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/util/PModelCache.java
Status: PORTED
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 239 — src/main/java/com/arcanc/pulselib/util/PRenderTypes.java

Source path: src/main/java/com/arcanc/pulselib/util/PRenderTypes.java
Source change: M
Subsystem: RENDER_CORE
Target path: src/main/java/com/arcanc/pulselib/util/PRenderTypes.java
Status: ADAPTED_1_21_1
Notes: source behavior: create material-specific render layers and identify translucency/OIT work. API difference: 1.21.1 RenderType and render-pipeline APIs differ from the source version. 1.21.1 implementation: retain the target RenderType factories and route queue submissions by native transparency state. target symbol: PRenderTypes.RenderTypeProvider and PRenderQueue.
## 240 — src/main/java/com/arcanc/pulselib/util/PResourceCache.java

Source path: src/main/java/com/arcanc/pulselib/util/PResourceCache.java
Source change: A
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/util/PResourceCache.java
Status: PORTED
Notes: New source API; port after its owning subsystem is scheduled.

## 241 — src/main/java/com/arcanc/pulselib/util/PTextureCache.java

Source path: src/main/java/com/arcanc/pulselib/util/PTextureCache.java
Source change: D
Subsystem: RESOURCE_MODEL
Target path: src/main/java/com/arcanc/pulselib/util/PTextureCache.java
Status: PORTED
Notes: Deletion requires a 1.21.1 hook review; do not remove solely because 26.x no longer needs it.

## 242 — src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchor.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchor.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchor.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 243 — src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 244 — src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentBinding.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentBinding.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentBinding.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 245 — src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentDefinition.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentDefinition.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentDefinition.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 246 — src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentLayer.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentLayer.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentLayer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 247 — src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSource.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSource.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSource.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 248 — src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSources.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSources.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSources.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 249 — src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachments.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachments.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachments.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 250 — src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolver.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolver.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolver.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 251 — src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolvers.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolvers.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolvers.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 252 — src/main/java/com/arcanc/pulselib/util/attachments/PTransform.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/PTransform.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/PTransform.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 253 — src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 254 — src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAttachmentLayer.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAttachmentLayer.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAttachmentLayer.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 255 — src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java
Source change: M
Subsystem: PLAYER_ANIMATION
Target path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 256 — src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java
Source change: M
Subsystem: ATTACHMENTS_ARMOR
Target path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java
Status: ALREADY_PRESENT
Notes: ALREADY_PRESENT: source delta is documentation-only or the target attachment/armor API already preserves the runtime behavior through the 1.21.1 direct renderer path.
## 257 — src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java

Source path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java
Source change: M
Subsystem: ATTACHMENTS_ARMOR
Target path: src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java
Status: ADAPTED_1_21_1
Notes: source behavior: retain the player animation, frame, attachment, or armor responsibility. API difference: source relies in places on modern render states or player hooks. 1.21.1 implementation: use direct PlayerModel/LivingEntityRenderer/PlayerRenderer callbacks and native NeoForge client extensions. target symbol: PPlayerAnimations, PPlayerAnimationFrame, PPlayerAnimatedAttachmentLayer, or PLibArmorHandler.
## 258 — src/main/java/com/arcanc/pulselib/util/helpers/PLibCodecs.java

Source path: src/main/java/com/arcanc/pulselib/util/helpers/PLibCodecs.java
Source change: M
Subsystem: OTHER
Target path: src/main/java/com/arcanc/pulselib/util/helpers/PLibCodecs.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 259 — src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java

Source path: src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java
Source change: M
Subsystem: OTHER
Target path: src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 260 — src/main/java/com/arcanc/pulselib/util/helpers/PLibParserHelper.java

Source path: src/main/java/com/arcanc/pulselib/util/helpers/PLibParserHelper.java
Source change: M
Subsystem: OTHER
Target path: src/main/java/com/arcanc/pulselib/util/helpers/PLibParserHelper.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 261 — src/main/java/com/arcanc/pulselib/util/helpers/PLibRenderHelper.java

Source path: src/main/java/com/arcanc/pulselib/util/helpers/PLibRenderHelper.java
Source change: M
Subsystem: OTHER
Target path: src/main/java/com/arcanc/pulselib/util/helpers/PLibRenderHelper.java
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 262 — src/main/resources/assets/pulselib/glmodels.zip

Source path: src/main/resources/assets/pulselib/glmodels.zip
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: src/main/resources/assets/pulselib/glmodels.zip
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 263 — src/main/resources/assets/pulselib/template/player/player_model_template.bbmodel

Source path: src/main/resources/assets/pulselib/template/player/player_model_template.bbmodel
Source change: A
Subsystem: DEMO_RESOURCES_DOCS
Target path: src/main/resources/assets/pulselib/template/player/player_model_template.bbmodel
Status: PENDING
Notes: New source API; port after its owning subsystem is scheduled.

## 264 — src/main/resources/assets/pulselib/template/player/player_model_template.gltf

Source path: src/main/resources/assets/pulselib/template/player/player_model_template.gltf
Source change: A
Subsystem: DEMO_RESOURCES_DOCS
Target path: src/main/resources/assets/pulselib/template/player/player_model_template.gltf
Status: PENDING
Notes: New source API; port after its owning subsystem is scheduled.

## 265 — src/main/resources/assets/pulselib/textures.zip

Source path: src/main/resources/assets/pulselib/textures.zip
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: src/main/resources/assets/pulselib/textures.zip
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 266 — src/main/resources/pulselib.mixins.json

Source path: src/main/resources/pulselib.mixins.json
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: src/main/resources/pulselib.mixins.json
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 267 — wiki/api-reference.md

Source path: wiki/api-reference.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/api-reference.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 268 — wiki/armor-and-attachments.md

Source path: wiki/armor-and-attachments.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/armor-and-attachments.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 269 — wiki/basic.md

Source path: wiki/basic.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/basic.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 270 — wiki/installation.md

Source path: wiki/installation.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/installation.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 271 — wiki/model-loaders.md

Source path: wiki/model-loaders.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/model-loaders.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 272 — wiki/modeldata.md

Source path: wiki/modeldata.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/modeldata.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 273 — wiki/player-animations.md

Source path: wiki/player-animations.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/player-animations.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 274 — wiki/pulselib-items.md

Source path: wiki/pulselib-items.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/pulselib-items.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 275 — wiki/render-types-and-queue.md

Source path: wiki/render-types-and-queue.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/render-types-and-queue.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 276 — wiki/renderers.md

Source path: wiki/renderers.md
Source change: M
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/renderers.md
Status: PENDING
Notes: Port the SOURCE^ -> SOURCE delta; reconcile API at implementation time.

## 277 — wiki/resources.md

Source path: wiki/textures-and-emissive.md -> wiki/resources.md
Source change: R
Subsystem: DEMO_RESOURCES_DOCS
Target path: wiki/resources.md
Status: PENDING
Notes: Rename delta; retain source behavior while adapting the target path.
