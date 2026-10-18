# Player animations

## First-person integration (Minecraft 1.21.1)

First-person output is opt-in with `firstPerson(PPlayerFirstPersonSettings.ENABLED)` and requires the `FIRST_PERSON_CAMERA` anchor. Bind `RIGHT_ARM` and `LEFT_ARM` for the vanilla arms, and use `RIGHT_ITEM` and `LEFT_ITEM` anchors for held items. PulseLib preserves the native 1.21.1 `ItemInHandRenderer` equip, swing, use, map, display-context, and left/right hand pipeline; it replaces only the final arm or item spatial submission when the presentation supplies an animated pose.

`PFirstPersonRenderContexts` carries a per-hand presentation through the renderer. `PVanillaFirstPersonArmResolver` and `PVanillaFirstPersonItemResolver` adapt camera-relative animation transforms to the vanilla arm and item origins. `PPlayerFirstPersonRenderer` draws first-person anchor and mesh attachments before the native buffer batch closes. `PFirstPersonCameraMode.ANIMATED` applies the `FIRST_PERSON_CAMERA` delta in `Camera.setup`; `VANILLA` leaves the camera unchanged.

Arms, items, custom anchors, and additional meshes share one `PFirstPersonPresentation`: each transform is `inverse(currentCameraModelMatrix) * currentBoneModelMatrix`. Both matrices include the full parent hierarchy. The resulting commands are already in VIEW space; ROOT and camera motion are not applied again by the hand or extras renderer. See [Animated camera](#animated-camera) for camera-origin displacement and [Transform spaces](#transform-spaces) for the public frame APIs.

`Builder.itemRenderPolicy(...)` decides whether a physical `InteractionHand` item is suppressed: `ItemRenderPolicy.RENDER` is the default and `HIDE` hides it. `Builder.itemVisibility(controller, policy)` adds a phase-based decision using the named controller's interpolated time in seconds. Either policy produces a per-hand hidden presentation while preserving vanilla map handling and equip/swing/use transforms for visible items.

Register custom first-person anchor renderers in `IPulseClientPlugin.registerAnimatedAttachments(PAnimatedAttachmentRegistrar)`. The callback receives the animated transform, its weight, `PoseStack`, `MultiBufferSource`, and a `firstPerson` flag.

Player animations modify the vanilla player model after Minecraft has prepared its normal walk, swim, crouch, and item-use pose. They work for both classic and slim skins, including hats, jackets, sleeves, and pants layers.

The API is client-only. Register definitions in `IPulseClientPlugin.registerPlayerAnimations(PPlayerAnimationRegistrar)`:

```java
@PulsePlugin
public final class ExamplePlayerAnimations implements IPulseClientPlugin
{
	private static final PModelData COMBAT_MODEL = PModelData.direct(
			ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "player/combat"),
			PGeckoModelLoader.INSTANCE.id());

	private static final PRawAnimation IDLE = PRawAnimation.begin().
			thenLoop("animation.combat.idle").
			build();

	@Override
	public ResourceLocation getPluginId()
	{
		return ResourceLocation.fromNamespaceAndPath("examplemod", "players");
	}

	@Override
	public void registerModels(PModelRegistrar registrar)
	{
		registrar.model(COMBAT_MODEL).texture("0",
				ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "player/combat"));
	}

	@Override
	public void registerPlayerAnimations(PPlayerAnimationRegistrar animations)
	{
		animations.register(
				ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "combat_pose"),
				PPlayerAnimationDefinition.builder(COMBAT_MODEL).
						when(player -> player.getMainHandItem().is(MyItems.KATANA.get())).
						bind(PPlayerPart.BODY, "body").
						bind(PPlayerPart.RIGHT_ARM, "right_arm").
						mask(PPlayerPart.BODY, PPlayerPart.RIGHT_ARM).
						blendMode(PPlayerAnimationBlendMode.ADDITIVE_LOCAL).
						weight(0.75f).
						controllers(registrar -> registrar.add("combat", () -> state ->
						{
							state.controller().play(IDLE);
							return ControllerState.PLAY;
						})).
						build());
	}
}
```

The Gecko loader is built in. This example registers its fallback material reference `0` as `assets/examplemod/textures/player/combat.png`. Declare every material reference used by your own model in the same `registerModels(PModelRegistrar)` callback.

Controllers receive a `PPlayerAnimationInstance` as their animatable. Use `state.animatable().player()` to read the current player. PulseLib creates one instance for every `(player UUID, definition id)` pair, so remote players and multiple definitions never share a controller timeline.

Only players currently tracked by the client are ticked. A player visible merely in the multiplayer tab list has no client entity and does not allocate animation state.

For a client packet handler or another explicit client action, use a runtime handle instead of reaching into an animation manager:

```java
PPlayerAnimationHandle animation = PPlayerAnimations.getHandle(player, COMBAT_POSE_ID);
if (animation != null)
{
	animation.play("combat", ATTACK);
}
```

`play`, `stop`, `pause`, `resume`, `stopAll`, and state checks address a named controller. `controller(name)` is available for advanced controller-specific operations without exposing an `AnimManagerKey`. Game-state synchronization itself remains the owning mod's responsibility.

## Bindings and masks

`bind(part, boneName)` maps a bone in the animation model to a semantic player part. `PPlayerPart` values are `ROOT`, `HEAD`, `BODY`, `RIGHT_ARM`, `LEFT_ARM`, `RIGHT_LEG`, and `LEFT_LEG`.

`ROOT` applies a visual animation delta to the complete third-person player render, including its feature layers. Use it for an emote that moves or rotates the visible model, such as a flip. `rootPivot(x, y, z)` selects its rotation/scale pivot in source-model blocks. Choose the pivot in the rig's own coordinate basis. In first person, `FIRST_PERSON_CAMERA` supplies the shared view frame; arms, items, and meshes inherit every parent in their own model-space chains before conversion to that frame. An empty off-hand is rendered when its `LEFT_ARM` or `RIGHT_ARM` binding has an active sampled transform.

Each semantic part applies to both the base part and the matching outer skin layer. For example, `RIGHT_ARM` transforms `rightArm` and `rightSleeve` together.

`mask(...)` is an explicit allow-list. It is useful when one skeleton contains more animated bones than a particular definition should own. If it is not supplied, every bound part is enabled. A dynamic mask can decide independently for every part and render frame:

```java
.mask((player, part, partialTick) ->
		part != PPlayerPart.HEAD || !player.isUsingItem())
```

## Visual Root Motion

PulseLib applies Root Motion to visual model translation, rotation, and scale only. Even if an animation moves the visible model several blocks away, `Entity.position()`, `getDeltaMovement()`, the bounding box, collision dimensions, and server position stay unchanged by PulseLib. Animated camera movement is also client-side and visual; it does not change the player's physical eye position.

`PRootMotionRuntime` and `PRootMotionDelta` extract animation-space displacement data. The consuming mod decides whether and how to use those values for gameplay movement, collision handling, or synchronization. PulseLib does not automatically apply the extracted delta to an entity or provide a server Root Motion movement system.

For a hierarchy such as `ROOT → BODY → RIGHT_ARM → ITEM_ANCHOR`, model-space sockets inherit ROOT and every intermediate parent. The native third-person player stack already contains the weighted visual ROOT delta, including `rootPivot`. Native anchors and extra meshes therefore use a current-root-relative matrix on that stack, so ROOT contributes exactly once. This also holds when several definitions contribute weighted ROOT transforms. The existing masks, part/bone weights, blend modes, and crossfades retain their behavior.

## Transform spaces

`PPlayerAnimationInstance.sampleFrame(partialTick)` returns the evaluated frame used by the player-animation consumers. Let `C_b` and `B_b` denote the full current and bind hierarchy matrices of bone `b`, and let `R` be the bound ROOT.

| API / consumer | Space | ROOT handling |
|---|---|---|
| `canonicalModelDelta(b)` | TRS channel delta between `C_R^-1 * C_b` and `B_R^-1 * B_b` for children; ROOT itself supplies a model-space channel delta | Excluded from child deltas |
| `rootRelativeTransform(b)` | Current pose in the ROOT bind frame: `B_R^-1 * C_b` | Animated ROOT included; use when the outer stack has not already applied it |
| `currentRootRelativeTransform(b)` / `currentRootRelativeMatrix(b, destination)` | Current ROOT frame: `C_R^-1 * C_b` | Excluded; use with an outer stack that already contains visual ROOT |
| `actionTransform(anchor)` | Full source MODEL-space TRS socket | ROOT and all intermediate parents included; this public contract is unchanged |
| `modelMatrix(b, destination)` / `bindMatrix(b, destination)` | Exact current / bind model-space hierarchy product | Included |
| Native third-person animated anchors and automatic meshes | Current-root-relative, converted to the appropriate player/geometry axes | Outer player stack applies visual ROOT once |
| First-person arms, items, anchors, and meshes | Source-model camera-relative VIEW frame: `C_camera^-1 * C_b` | Shared hierarchy is removed once by the camera frame; no additional ROOT/camera application |

For a rootless rig, the root-relative APIs retain the HEAD bind frame as their reference when HEAD is bound; otherwise they retain model space. MODEL-space sockets are not entity world-space positions. Source glTF matrices also need the player-axis conversion at native rendering boundaries; the built-in consumers perform this conversion.

`com.arcanc.pulselib.content.model.animation.PTransform` stores translation, quaternion rotation, and scale channels. Rotated nonuniform scales can produce shear, which a TRS snapshot cannot represent. Use the frame's matrix APIs for exact hierarchy products in custom matrix rendering; built-in animated anchor and mesh consumers retain those matrices. Vanilla `ModelPart` and arm/item command blending keep their existing TRS representation.

## Animated camera

`FIRST_PERSON_CAMERA` can be a descendant of ROOT or of intermediate bones. In `PFirstPersonCameraMode.ANIMATED`, its position and orientation inherit the full animated hierarchy. The camera uses the same visual ROOT/pivot convention as the native player. Camera definition weight and first-person activation still control how its pose contributes.

`cameraAnchorModelDelta()` returns displacement **at the camera origin**, plus inherited orientation and scale channels. Its translation is no longer the translation column of the affine delta at model zero. Bind and current camera matrices are first expressed in the same normalized visual reference frame. For those matrices `B_visual` and `C_visual`:

```text
D = C_visual * inverse(B_visual)
p_bind = B_visual * (0, 0, 0, 1)
displacement = D * p_bind - p_bind
```

With ROOT, `C_visual = V_ROOT * inverse(C_ROOT) * C_camera` and `B_visual = inverse(B_ROOT) * B_camera`; `V_ROOT` is the visual ROOT delta with the definition's `rootPivot`. A rootless rig uses the constant HEAD bind reference for both poses, or keeps model space when HEAD is absent. Orientation is the inherited current quaternion relative to the inherited bind quaternion in that same reference frame.

Rotating the camera bone around its own origin changes orientation without moving the camera. Rotating ROOT moves an offset camera anchor along its actual arc about the visual ROOT pivot. This retains inherited translation, rotation, and scale effects without introducing a second displacement from rotation about the bind offset.

The native camera boundary converts Gecko/glTF source axes through player axes into Minecraft camera axes and composes the animated orientation with the vanilla camera orientation. Fabric and NeoForge use the same signed-roll convention. Export glTF using its normal axes; the player pipeline performs the conversion.

With `PFirstPersonCameraMode.VANILLA`, the animated anchor does not modify the actual camera position or rotation. `FIRST_PERSON_CAMERA` still supplies the authored model-to-VIEW frame for first-person animation output. Choose the mode through `new PPlayerFirstPersonSettings(true, transitionIn, transitionOut, cameraMode)`; transition durations are in ticks.

## Animated anchors and automatic meshes

Register custom anchor renderers through `IPulseClientPlugin.registerAnimatedAttachments(PAnimatedAttachmentRegistrar)`. PulseLib passes a `PPlayerAnimatedAttachmentContext` with a `PoseStack` already positioned at the anchor. Render geometry relative to that local origin. Applying `context.transform()` to the supplied stack again repeats the anchor transform.

In third person, `context.transform()` retains the full source MODEL-space socket contract even though the supplied stack uses a current-root-relative matrix over the outer visual ROOT. In first person, the context transform is already camera-relative VIEW-space. The `firstPerson()` flag identifies the path; see [Armor and attachments](armor-and-attachments.md#player-animation-anchors-and-meshes).

Mesh subtrees that are not mapped to vanilla player parts can be discovered automatically from an active animation rig. A static mesh such as `ROOT → RIGHT_ARM → BALL` follows its animated parents without needing its own BALL channel. Static mesh descendants on an active bound rig are also included when only the camera animates, so first-person meshes stay in the same view frame as arms and items. `Builder.meshAttachment(boneName)` selects explicit attachment subtrees when automatic discovery is unsuitable.

## Blending and weights

Definitions run in ascending `priority`; registrations with the same priority are ordered by their id. The default is `ADDITIVE_LOCAL`, which adds the sampled position and rotation to the pose already produced by vanilla and earlier definitions. It is a good default for recoil, breathing, and gestures.

The available modes are:

* `OVERRIDE` blends from the original vanilla pose to the sampled pose.
* `ADDITIVE_LOCAL` and `ADDITIVE_MESH_SPACE` add sampled transforms to the existing pose.
* `MULTIPLY_SCALE` affects scale only.
* `DIFFERENCE` subtracts translation and applies inverse rotation/scale.

Use `OVERRIDE` for emotes or stances that should replace vanilla limb motion.

The definition weight is clamped to `[0, 1]`. A constant weight is sufficient for most cases:

```java
.weight(0.4f)
```

For a dynamic blend, provide a function evaluated every render pass:

```java
.weight((player, partialTick) -> player.isCrouching() ? 1.0f : 0.25f)
```

`partWeight(...)` multiplies that definition weight only for the selected part. It accepts either a constant or the same dynamic weight function:

```java
.weight(0.8f).
partWeight(PPlayerPart.HEAD, 0.25f).
partWeight(PPlayerPart.RIGHT_ARM,
		(player, partialTick) -> player.isUsingItem() ? 1.0f : 0.0f)
```

Layer several definitions with different masks, priorities, modes, definition weights, and part weights to combine independent actions.

## Activation crossfades and cycle synchronization

By default a definition starts and stops immediately when its `when(...)` predicate changes. Add `crossfade` to blend that activation in ticks:

```java
.crossfade(4.0f, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT)
```

`FROM_CURRENT` reverses smoothly from the current fade amount, `RESTART` starts the new fade from its endpoint, and `COMPLETE_CURRENT` lets the current fade finish before accepting a new predicate change.

Definitions with the same non-empty `syncGroup` keep similarly named looping controllers at the same normalized cycle phase for each player:

```java
.syncGroup("combat")
```

Use this for layered models whose walk, idle, or other cyclic animations must stay aligned. `boneWeight(boneName, weight)` can additionally scale one bound bone independently of its semantic player-part weight.

## Mesh deformers

Attach a `PDeformerStack` directly to a definition with `deform(part, stack, values)`. It is evaluated only while that definition contributes; its values are interpolated with the render `partialTick` and automatically fade to each channel's default when the animation fades out.

```java
private static final PChannelReference<Float> KNEE_BEND =
		new PChannelReference<>("knee_bend", 0.0f);

// `BENT_LEG` is a compiled PDeformerStack, for example one PBendDeformer.
PPlayerAnimationDefinition.builder(MODEL).
		bind(PPlayerPart.RIGHT_LEG, "right_leg").
		deform(PPlayerPart.RIGHT_LEG, BENT_LEG, (context, reference) ->
		{
			if (!reference.name().equals(KNEE_BEND.name()))
			{
				return reference.defaultValue();
			}
			return sampleKnee(context.controllerSeconds("combat"));
		}).
		build();
```

The callback receives `PPlayerAnimationDeformerContext`: `player()`, `definition()`, `partialTick()`, `weight()`, `isPlaying(name)`, `controllerTicks(name)`, and `controllerSeconds(name)`. More than one deformer may target the same part; they run in declaration order. The lower-level global `PPlayerMeshDeformers.register(...)` remains available for a deformation that is not part of an animation definition.

See [Mesh deformers](mesh-deformers.md) for built-in operations, subdivision, normals, custom meshes, and custom deformer types.

## Model conventions and limitations

The model only needs a skeleton and animations; mesh data is optional. Gecko positions are authored in model pixels and converted to blocks by the loader; the player boundary converts those positions to vanilla model pixels. Rotation, position, and scale channels are supported. Standard glTF assets keep their normal right-handed Y-up coordinates: the player pipeline converts their position and quaternion axes to Minecraft's mirrored player render space. Do not pre-flip an exported glTF player animation. The skeleton resolver evaluates bind pose and parent transforms; a bound child therefore inherits transforms of intermediate animation bones. A bound `ROOT` is applied once to the render stack and is excluded from child deltas.

The API affects the whole player model in third person and arms in first person. It restores position, rotation, and scale immediately after every draw, preventing a pose from leaking into a different player or another render layer.

`populateMolangContext(...)` can add player-specific Molang queries:

```java
.populateMolangContext((player, instance, controller, context, partialTick) ->
		context.query("is_sneaking", player.isCrouching() ? 1.0f : 0.0f))
```

As with other client-side animation state, multiplayer gameplay events are the owning mod's responsibility to synchronize. The animation definition observes only state available on that client.
