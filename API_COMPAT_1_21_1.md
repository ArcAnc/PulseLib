# API compatibility map: source 1976c6423786e38caa9d96c3f385bd469e887e49 to 1.21.1

This is an implementation map, not a reason to omit source behavior. It is based on the source delta and the 1.21.1 NeoForge 21.1 API used by this branch.

## ItemModelResolver

26.x concept: ItemModelResolver
source responsibility: resolve an item stack into the modern item-model graph and expose its ModelManager to PulseLib's special-model integration.
available in 1.21.1: NO
1.21.1 equivalent: ItemRenderer.getModel(stack, level, entity, seed), ModelManager, BakedModel and ItemOverrides.
probable target class/method: ItemRenderer mixin at getModel/render/renderStatic, or PItemRenderer's existing BlockEntityWithoutLevelRenderer path.
notes: Obtain the selected BakedModel and ItemDisplayContext at ItemRenderer/ItemInHandRenderer call sites. Do not add a resolver abstraction merely to mirror 26.x.

## ItemStackRenderState

26.x concept: ItemStackRenderState
source responsibility: carry item stack, display context, light, overlay, foil and outline data from extraction into submission; ItemStackRenderStateAccessor reads displayContext.
available in 1.21.1: NO
1.21.1 equivalent: the arguments of ItemRenderer.render and renderStatic, plus ItemInHandRenderer.renderItem.
probable target class/method: PItemRenderer.renderByItem and a scoped 1.21.1 render context around ItemRenderer.render; first-person interception at ItemInHandRenderer.renderArmWithItem.
notes: Preserve item pose behavior by retaining these call arguments in a PulseLib context/state object. The absent accessor does not justify skipping first-person item transforms.

## SpecialModelWrapper and SpecialModelRenderer

26.x concept: SpecialModelWrapper/SpecialModelRenderer
source responsibility: let PItemRenderer extract a typed PItemRenderState, then submit custom geometry through the modern special-model path.
available in 1.21.1: NO
1.21.1 equivalent: BakedModel.isCustomRenderer together with BlockEntityWithoutLevelRenderer.renderByItem; ItemRenderer delegates custom item rendering through this older path.
probable target class/method: PItemRenderer extends BlockEntityWithoutLevelRenderer; ItemRenderer.render and existing item renderer registration.
notes: Keep the existing BlockEntityWithoutLevelRenderer hook. Do not port SpecialModelWrapper accessors/extractors as 1.21.1 mixins.

## BlockEntityRenderState

26.x concept: BlockEntityRenderState and BlockEntityRenderer<T, RS>.extractRenderState/submit
source responsibility: separate block-entity data extraction from rendering and preserve block state, break overlay, camera position and partial tick.
available in 1.21.1: NO
1.21.1 equivalent: BlockEntityRenderer.render(T, float, PoseStack, MultiBufferSource, int, int), called by BlockEntityRenderDispatcher.render.
probable target class/method: PBlockRenderer.render and, if needed, a PulseLib-owned ephemeral render data object populated at its entry.
notes: Read block state from the live BlockEntity and use the render method arguments. Keep any old accessor only where a live 1.21.1 hook requires it.

## Modern entity render states

26.x concept: EntityRenderState, LivingEntityRenderState, AvatarRenderState and state-based LivingEntityRenderer.submit/setupAnim.
source responsibility: extract entity animation data once, apply model poses and root transforms during state submission, and render player attachments.
available in 1.21.1: NO
1.21.1 equivalent: generic LivingEntityRenderer<T, M>.render(T, yaw, partialTick, PoseStack, MultiBufferSource, light), PlayerRenderer and PlayerModel.setupAnim(entity, ...).
probable target class/method: existing LivingEntityRendererMixin, PlayerRendererMixin, PlayerRootTransformMixin, and PEntityRenderer.render.
notes: Continue to apply/restore poses around the live entity render call. The source deletion of PlayerRendererMixin is not applicable by itself: 1.21.1 still needs its renderHand bridge.

## Modern item rendering pipeline

26.x concept: item-model resolution, ItemStackRenderState, SpecialModelRenderer and SubmitNodeCollector submission.
source responsibility: preserve custom item geometry, GUI item batching and first-person display contexts.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: ItemRenderer.getModel/render/renderStatic, BakedModel, ItemTransforms/ItemDisplayContext and BlockEntityWithoutLevelRenderer.
probable target class/method: PItemRenderer.renderByItem, ItemInHandRenderer.renderItem and ItemRenderer.render.
notes: ItemDisplayContext exists with the needed FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND and GUI constants. BakedModel also exists, but it is the old quad/custom-renderer model rather than the 26.x item-model graph.

## Modern render extraction and submission

26.x concept: extractRenderState followed by submit through SubmitNodeCollector, CameraRenderState and deferred feature submission.
source responsibility: stage PulseLib geometry until the appropriate world, entity, GUI or first-person render stage.
available in 1.21.1: NO
1.21.1 equivalent: immediate calls with PoseStack and MultiBufferSource/VertexConsumer, plus the existing PRenderQueue stage flushes.
probable target class/method: PRenderer/PItemRenderer/PBlockRenderer/PEntityRenderer and GameRendererMixin after ItemInHandRenderer.renderHandsWithItems.
notes: Preserve queue ordering and first-person flush/composite behavior, but submit through MultiBufferSource rather than recreating SubmitNodeCollector.

## Modern player renderer hooks

26.x concept: AvatarRenderer.renderRightHand/renderLeftHand and feature dispatcher inside renderHandsWithItems.
source responsibility: replace or suppress each physical arm/item transform and inject animated attachments around first-person submission.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: PlayerRenderer.renderRightHand/renderLeftHand, private renderHand, and ItemInHandRenderer.renderPlayerArm/renderArmWithItem.
probable target class/method: ItemInHandRendererMixin around renderHandsWithItems, renderArmWithItem, renderPlayerArm and renderItem.
notes: The target has direct hand render methods but no AvatarRenderer or feature dispatcher. ItemInHandRendererMixin wraps its private arm/item path and its direct PlayerRenderer hand calls, so the accessor and PlayerRendererMixin are removed from the active config to avoid applying a pose twice.

## Block entity and entity renderer interfaces

26.x concept: state-typed BlockEntityRenderer and EntityRenderer submission contracts.
source responsibility: allow PulseLib render states to retain animation key, model, light and overlay through extraction.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: live generic BlockEntityRenderer<T>.render and EntityRenderer<T>.render.
probable target class/method: PBlockRenderer.render, PEntityRenderer.render and local state/data holders that do not extend vanilla render-state classes.
notes: Port library-facing state APIs where useful, but adapt the Minecraft boundary to live render arguments.

## PoseStack and transform data

26.x concept: PoseStack used with state submission and PFirstPersonPoseStack local-pose replacement.
source responsibility: apply canonical model-space arm, item and camera transforms.
available in 1.21.1: YES
1.21.1 equivalent: PoseStack.last().pose()/normal(), pushPose/popPose, translate, scale and mulPose.
probable target class/method: ItemInHandRendererMixin, CameraMixin and first-person helper classes.
notes: Matrix operations are available. Convert only coordinate conventions required by the source behavior; do not introduce 26.x renderer state types.

## MultiBufferSource and RenderType

26.x concept: SubmitNodeCollector plus renderer.rendertype.RenderType.
source responsibility: select render layers and submit or defer mesh geometry.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: MultiBufferSource.getBuffer(RenderType), VertexConsumer and net.minecraft.client.renderer.RenderType.
probable target class/method: PRenderQueue, PRenderTypes and PItemRenderer/PBlockRenderer/PEntityRenderer.
notes: RenderType factories and PoseStack are present, but submission is immediate. Preserve material/layer selection and use target package names.

## Camera and GameRenderer

26.x concept: CameraRenderState-aware submission, Camera.alignWithEntity and GameRenderer first-person render hook.
source responsibility: apply animated head/camera offset and flush first-person geometry after vanilla hands/items.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: Camera.setup(level, entity, detached, reverse, partialTick), Camera position/rotation accessors, and GameRenderer.renderItemInHand(Camera, float, Matrix4f).
probable target class/method: existing CameraMixin and GameRendererMixin.
notes: The target already has a verified inject point immediately after ItemInHandRenderer.renderHandsWithItems; retain it and adapt camera math to setup rather than alignWithEntity.

## BakedModel and ItemDisplayContext

26.x concept: BakedModel adjacent to the new item model pipeline, with ItemDisplayContext retained in ItemStackRenderState.
source responsibility: model choice and transform-space selection for custom items.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: BakedModel.getQuads/isCustomRenderer/getTransforms/getOverrides and ItemDisplayContext.
probable target class/method: ItemRenderer.getModel, ItemRenderer.render and PItemRenderer.renderByItem.
notes: ItemDisplayContext is compatible enough to preserve first-person/GUI decisions. Model resolution and custom renderer dispatch must use the 1.21.1 BakedModel contract.

## Source deletions requiring target retention review

26.x concept: deleted ItemInHandRendererAccessor, PlayerRendererMixin and PTextureCache.
source responsibility: 26.x removes old private-arm access because AvatarRenderer exposes hand methods; replaces texture cache naming with PResourceCache.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: ItemInHandRenderer directly invokes PlayerRenderer.renderRightHand/renderLeftHand; PTextureCache must be assessed independently of API migration.
probable target class/method: ItemInHandRendererMixin wrap operations and resource-cache users.
notes: The target retains neither deleted hook in its mixin config: direct ItemInHandRenderer wrapping replaces their responsibility without duplicate PlayerRenderer pose application.

## Verified native 1.21.1 backend mappings

26.x concept: block-entity extraction and SubmitNodeCollector submission.
source responsibility: retain live block inputs, animate registered meshes, and resolve their materials from the owning model resource.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: BlockEntityRenderer.render(T, float, PoseStack, MultiBufferSource, int, int) and RenderLevelStageEvent.Stage.
probable target class/method: PBlockRenderer.render, PBlockRenderer.submitBone, and PRenderStagesHandler.renderLevelStages.
notes: The renderer reads the live block entity, evaluates controllers and Molang contexts, resolves PMeshRenderMaterial through PResourceCache.ATLAS_LOCATION, then flushes solid and translucent work at the corresponding 1.21.1 level stages.

26.x concept: EntityRenderState extraction and deferred entity submission.
source responsibility: preserve entity animation data, per-bone attachments, layers, lighting, overlays, and model-local materials.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: EntityRenderer.render(T, yaw, partialTick, PoseStack, MultiBufferSource, light).
probable target class/method: PEntityRenderer.render, PEntityRenderer.perBoneSubmit, and PEntityRenderLayer.submit.
notes: Entity data is read during the live renderer call. Bone transforms and layer anchors are built there, and mesh submissions use PResourceCache.ATLAS_LOCATION before the entity queue flush.

26.x concept: ItemModelResolver, ItemStackRenderState, and SpecialModelWrapper.
source responsibility: select an item model, retain stack/display inputs, and submit special animated geometry.
available in 1.21.1: NO
1.21.1 equivalent: BakedModel.isCustomRenderer and BlockEntityWithoutLevelRenderer.renderByItem(ItemStack, ItemDisplayContext, PoseStack, MultiBufferSource, int, int).
probable target class/method: PItemRenderer.renderByItem and PItemRenderer.trueSubmit.
notes: The native BEWLR callback supplies ItemStack, ItemDisplayContext, light, and overlay directly. GUI meshes are drawn immediately; world and hand meshes are staged in PRenderQueue, with the first-person queue flushed by GameRendererMixin after renderHandsWithItems.

26.x concept: extracted animation pose consumed by renderer submission.
source responsibility: apply pose evaluation and visibility tracks consistently to immediate item rendering.
available in 1.21.1: DIFFERENT
1.21.1 equivalent: PoseStack traversal during the direct renderer callback.
probable target class/method: PBakedModel.instantDraw and PBakedBone.instantDraw.
notes: PBakedModel now creates one PAnimationPoseResolver for the draw traversal, so visibility tracks and resolved local transforms apply to GUI and other immediate paths without a modern vanilla render state.

26.x concept: model texture lookup through PResourceCache.
source responsibility: ensure each renderer resolves mesh textures in the registered model resource context rather than through a global texture-name cache.
available in 1.21.1: YES
1.21.1 equivalent: PResourceCache.resolve, PMeshTextureVariants.resolve, TextureAtlas, and RenderType.
probable target class/method: PBlockRenderer.submitBone, PEntityRenderer.submitBone, PItemRenderer.submitBone, and PBakedBone.drawMesh.
notes: Every target renderer resolves PMeshRenderMaterial and uses PResourceCache.ATLAS_LOCATION. Texture overrides are converted to the resource-cache sprite id by PMeshTextureVariants, so identically named relative textures remain scoped to their model resource.

## First-person integration hooks implemented for 1.21.1

| Source hook | Source responsibility | Why the source vanilla target is unavailable | 1.21.1 hook | Implementation |
| --- | --- | --- | --- | --- |
| `BlockEntityRenderStateAccessor` | Read extracted block state during deferred block rendering. | `BlockEntityRenderState` does not exist. | Live `BlockEntityRenderer.render`. | `PBlockRenderer` reads the live block entity and block state; no accessor is registered. |
| `CameraMixin` | Apply animated camera position and rotation after entity alignment. | 1.21.1 has `Camera.setup`, not `alignWithEntity`. | `CameraMixin#setup` tail. | Converts the animated `FIRST_PERSON_CAMERA` delta through `PFirstPersonCameraSpace`; camera mode is honored. |
| `CubeDefinitionMixin` | Bake deformable player cuboids. | Available with the same model builder API. | `CubeDefinition.bake`. | Existing cancellable bake hook returns `PDeformedCuboid` inside the scoped player layer bake. |
| `EntityModelSetMixin` | Scope deformable cube baking to player layers. | Available but has the direct `LayerDefinition.bakeRoot` call. | `EntityModelSet.bakeLayer` redirect. | Wraps only vanilla player and slim-player layers in `PDeformableCubeBakeScope`. |
| `GameRendererMixin` | Flush first-person queued geometry after hands/items. | 1.21.1 uses `Camera, float, Matrix4f`, not render states. | `GameRenderer.renderItemInHand` after `renderHandsWithItems`. | Flushes `PRenderQueue.FIRST_PERSON` and composites the target render target. |
| `GlBufferAccessor` | Read a modern `GlBuffer` handle for the new backend. | That modern class is absent. | Existing 1.21.1 buffer backend. | No fake accessor; the 1.21.1 renderer backend uses its native buffer path. |
| `HumanoidArmorLayerMixin` | Apply player deformers to armor geometry. | 1.21.1 armor is live-entity rendering, not render states. | `HumanoidArmorLayer.renderArmorPiece` after `setupModelAnimations`. | Applies `PPlayerMeshDeformers` to the actual armor model for the current player. |
| `ItemInHandRendererAccessor` | Old private bridge to vanilla arm rendering. | Source deletes it because newer renderer exposes direct hand submissions. | No accessor. | `ItemInHandRendererMixin` now wraps the direct 1.21.1 `PlayerRenderer.renderRightHand/renderLeftHand` calls, so the invoker is not registered. |
| `ItemInHandRendererMixin` | Scope a first-person pass; replace arm/item spatial submissions; render attachments. | Source uses `SubmitNodeCollector`, AvatarRenderer, and a feature dispatcher. | `renderHandsWithItems`, `renderArmWithItem`, `renderPlayerArm`, and `renderItem`. | A scoped `PFirstPersonRenderContexts` pass preserves vanilla equip/swing/use transforms; wraps final arm and item calls with `PoseStack` replacement, keeps hand side and maps distinct, and renders attachments before `BufferSource.endBatch`. |
| `ItemModelResolverAccessor` | Inspect modern item-model resolution. | `ItemModelResolver` is absent. | `ItemRenderer.renderStatic`/`BakedModel` and BEWLR. | First-person transforms wrap the existing `ItemInHandRenderer.renderItem` call without recreating item state machinery. |
| `ItemStackRenderStateAccessor` | Read modern display/render state. | `ItemStackRenderState` is absent. | `renderItem` arguments. | `ItemStack`, `ItemDisplayContext`, left-hand flag, light, and `MultiBufferSource` are carried directly by the 1.21.1 invocation. |
| `LivingEntityRendererMixin` | Apply and restore player animation frame in third person. | 1.21.1 renders live entities. | `LivingEntityRenderer.render` around `EntityModel.setupAnim`. | Applies `PPlayerAnimationFrame` poses, deformers, and root transform, then restores the model. |
| `ModelPartCubesMixin` | Expose cubes to player deformers. | Available with the same field. | `ModelPart.cubes` shadow. | Existing `PModelPartCubes` implementation supplies the live cube list. |
| `PlayerRendererMixin` | Old first-person arm bridge. | The old bridge would double-apply poses after direct item-renderer wrapping. | No separate player-renderer mixin. | `ItemInHandRendererMixin` owns first-person arm submission; third-person poses remain in `LivingEntityRendererMixin`. |
| `PlayerRootTransformMixin` | Apply player root transform during state-based player rendering. | The state renderer is absent. | `LivingEntityRenderer.render`. | `LivingEntityRendererMixin` calls `PPlayerAnimations.applyRoot` on the live `PoseStack`. |
| `SpecialModelWrapperAccessor` | Reach a modern special item renderer. | `SpecialModelWrapper` is absent. | `BakedModel.isCustomRenderer` and BEWLR. | Existing `PItemRenderer` handles custom models through the native 1.21.1 item renderer path. |
| `SpecialModelWrapperRenderStateExtractor` | Transfer special model data to modern item render state. | Neither wrapper nor render state exists. | Direct BEWLR and `ItemInHandRenderer.renderItem`. | No fake extraction layer; standard display-context arguments and `PItemRenderer` retain the required data. |
