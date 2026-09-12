# Player animations

Player animations modify the vanilla player model after Minecraft has prepared its normal walk, swim, crouch, and item-use pose. They work for both classic and slim skins, including hats, jackets, sleeves, and pants layers.

The API is client-only. Register definitions on the mod event bus through `PulseLibEvents.PlayerAnimationRegistrationEvent`:

```java
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ExamplePlayerAnimations {
    private static final PModelData COMBAT_MODEL = new PModelData.Builder(
            Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "player/combat"),
            "player",
            PGeckoModelLoader.INSTANCE.id()).build();

    private static final PRawAnimation IDLE = PRawAnimation.begin()
            .thenLoop("animation.combat.idle")
            .build();

    @SubscribeEvent
    public static void register(PulseLibEvents.PlayerAnimationRegistrationEvent event) {
        event.registration().register(
                Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "combat_pose"),
                PPlayerAnimationDefinition.builder(COMBAT_MODEL)
                        .when(player -> player.getMainHandItem().is(MyItems.KATANA.get()))
                        .bind(PPlayerPart.BODY, "body")
                        .bind(PPlayerPart.RIGHT_ARM, "right_arm")
                        .mask(PPlayerPart.BODY, PPlayerPart.RIGHT_ARM)
                        .blendMode(PPlayerAnimationBlendMode.ADDITIVE_LOCAL)
                        .weight(0.75f)
                        .controllers(registrar -> registrar.add("combat", () -> state -> {
                            state.controller().play(IDLE);
                            return ControllerState.PLAY;
                        }))
                        .build());
    }
}
```

The example uses the Gecko loader. Register `PGeckoModelLoader.INSTANCE` with `PModelCache` before the first client resource reload, as described in [Model loaders](model-loaders.md), or use a glTF skeleton instead.

Controllers receive a `PPlayerAnimationInstance` as their animatable. Use `state.animatable().player()` to read the current player. PulseLib creates one instance for every `(player UUID, definition id)` pair, so remote players and multiple definitions never share a controller timeline.

Only players currently tracked by the client are ticked. A player visible merely in the multiplayer tab list has no client entity and does not allocate animation state.

For a client packet handler or another explicit client action, use a runtime handle instead of reaching into an animation manager:

```java
PPlayerAnimationHandle animation = PPlayerAnimations.getHandle(player, COMBAT_POSE_ID);
if (animation != null)
    animation.play("combat", ATTACK);
```

`play`, `stop`, `pause`, `resume`, `stopAll`, and state checks address a named controller. `controller(name)` is available for advanced controller-specific operations without exposing an `AnimManagerKey`. Game-state synchronization itself remains the owning mod's responsibility.

## Bindings and masks

`bind(part, boneName)` maps a bone in the animation model to a semantic player part. `PPlayerPart` values are `ROOT`, `HEAD`, `BODY`, `RIGHT_ARM`, `LEFT_ARM`, `RIGHT_LEG`, and `LEFT_LEG`.

`ROOT` applies the bone transform to the complete third-person player render, including its feature layers. Use it for an emote that moves or rotates the whole player, such as a flip. `rootPivot(x, y, z)` selects its rotation pivot in model-space blocks; a value close to `(0, 0.9, 0)` rotates around the centre of a standing player. For the local player's camera, `ROOT` and `HEAD` are also sampled in first person. This camera adjustment is independent of the first-person hand renderer described below.

Each semantic part applies to both the base part and the matching outer skin layer. For example, `RIGHT_ARM` transforms `rightArm` and `rightSleeve` together.

`mask(...)` is an explicit allow-list. It is useful when one skeleton contains more animated bones than a particular definition should own. If it is not supplied, every bound part is enabled. A dynamic mask can decide independently for every part and render frame:

```java
.mask((player, part, partialTick) ->
        part != PPlayerPart.HEAD || !player.isUsingItem())
```

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
.weight(0.8f)
.partWeight(PPlayerPart.HEAD, 0.25f)
.partWeight(PPlayerPart.RIGHT_ARM,
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

Use this for layered models whose walk, idle, or other cyclic animations must stay aligned. `boneWeight(boneName, weight)` can additionally scale one bound bone independently of its semantic player-part weight. It also scales first-person arms and item anchors that use that bone.

## Mesh deformers

Attach a `PDeformerStack` directly to a definition with `deform(part, stack, values)`. It is evaluated only while that definition contributes; its values are interpolated with the render `partialTick` and automatically fade to each channel's default when the animation fades out.

```java
private static final PChannelReference<Float> KNEE_BEND =
        new PChannelReference<>("knee_bend", 0.0f);

// `BENT_LEG` is a compiled PDeformerStack, for example one PBendDeformer.
PPlayerAnimationDefinition.builder(MODEL)
        .bind(PPlayerPart.RIGHT_LEG, "right_leg")
        .deform(PPlayerPart.RIGHT_LEG, BENT_LEG, (context, reference) -> {
            if (!reference.name().equals(KNEE_BEND.name()))
                return reference.defaultValue();
            return sampleKnee(context.controllerSeconds("combat"));
        })
        .build();
```

The callback receives `PPlayerAnimationDeformerContext`: `player()`, `definition()`, `partialTick()`, `weight()`, `isPlaying(name)`, `controllerTicks(name)`, and `controllerSeconds(name)`. More than one deformer may target the same part; they run in declaration order. The lower-level global `PPlayerMeshDeformers.register(...)` remains available for a deformation that is not part of an animation definition.

See [Mesh deformers](mesh-deformers.md) for built-in operations, subdivision, normals, custom meshes, and custom deformer types.

## Model conventions and limitations

The model only needs a skeleton and animations; mesh data is optional. Bone positions from Gecko animations are converted from blocks to vanilla model pixels automatically. Rotation, position, and scale channels are supported. Standard glTF assets keep their normal right-handed Y-up coordinates: the player pipeline converts their position and quaternion axes to Minecraft's mirrored player render space. Do not pre-flip an exported glTF player animation. The skeleton resolver evaluates bind pose and parent transforms; a bound child therefore inherits transforms of intermediate animation bones. A bound `ROOT` is applied once to the render stack and is excluded from child deltas.

The API affects the whole player model in third person. It restores position, rotation, and scale immediately after every draw, preventing a pose from leaking into a different player or another render layer.

## First-person hands, items, and camera space

First-person hand rendering is opt-in. `PPlayerFirstPersonSettings.DISABLED` is the default. When a definition with `firstPerson(PPlayerFirstPersonSettings.ENABLED)` contributes for the local player, PulseLib cancels Minecraft's complete `renderHandsWithItems` call and draws the first-person view from the animation pose. Vanilla swing, equip, use, and empty-hand transforms are therefore not added automatically.

For its arms, items, meshes, or custom anchors to appear, an enabled definition needs a `FIRST_PERSON_CAMERA` anchor. It defines the origin used to convert those bones into first-person space. It does **not** move the Minecraft camera; bind `ROOT` and `HEAD` when the animation itself should move or rotate the local camera.

The first-person transform has two coordinate boundaries. PulseLib first expresses the camera-relative bone transform in Minecraft player-model coordinates. It then converts the result into the first-person renderer's view coordinates. For a glTF arm or item transform `M`, the resulting matrix is `M * C`, where `C = diag(-1, -1, 1)`. This is intentional: the transform receives vanilla arm or item geometry in player-model coordinates, so even an identity bone transform needs `C` to orient that geometry in view space. Animated glTF mesh attachments already have glTF vertex coordinates and therefore reduce to `M`. `C` currently has the same numeric values as the glTF-to-player conversion because it is self-inverse, but it is a separate player-model-to-first-person-view contract.

### PulseLib Player Action Rig

Use [`player_model_template.gltf`](../src/main/resources/assets/pulselib/template/player/player_model_template.gltf) or [`player_model_template.bbmodel`](../src/main/resources/assets/pulselib/template/player/player_model_template.bbmodel) as the starting skeleton for player actions. It contains `body`, `head`, `right_arm`, `left_arm`, and `fp_camera`, plus `right_hand` and `left_hand` item anchors. The file is intentionally skeleton-only: add preview meshes in Blockbench if they help authoring, but keep the named bone origins unchanged.

`right_arm` and `left_arm` have a fixed rig contract. Each bone origin must equal the local origin of the geometry-only vanilla `ModelPart` used by the first-person renderer. In particular, it is the arm pivot, not the player-model root, the center of the cuboid, or the hand grip. The template matches the normal 4×12×4 vanilla arms exactly after glTF conversion. PulseLib applies no per-arm correction matrix; a rig that moves either origin will move the rendered vanilla arm and its persistent attachments by the same offset.

```java
PPlayerAnimationDefinition.builder(MODEL)
        .bind(PPlayerPart.HEAD, "head")       // optional: drives the camera
        .bind(PPlayerPart.RIGHT_ARM, "right_arm")
        .bind(PPlayerPart.LEFT_ARM, "left_arm")
        .anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "fp_camera")
        .anchor(PPlayerAnimationAnchors.RIGHT_ITEM, "right_hand")
        .anchor(PPlayerAnimationAnchors.LEFT_ITEM, "left_hand")
        .firstPerson(PPlayerFirstPersonSettings.ENABLED)
        // controller setup
        .build();
```

`RIGHT_ARM` and `LEFT_ARM` supply the physical arms to draw; `RIGHT_ITEM` and `LEFT_ITEM` supply the physical first-person item-renderer origins. Minecraft maps its logical main/off hand to these physical left/right channels according to the player's main-arm setting.

Each channel has a `PFirstPersonRenderMode`. A channel without a sampled transform remains `VANILLA`, so Minecraft's per-hand swing, equip, use, map, and item-model transforms still apply even though PulseLib owns the outer first-person render call. A sampled arm or item channel becomes `ANIMATED` and PulseLib applies its camera-space transform. `HIDDEN` draws nothing. `PFirstPersonArmPose` and `PFirstPersonItemPose` enforce this boundary: only `ANIMATED` has a non-null transform.

An item anchor is applied before Minecraft renders the item with `FIRST_PERSON_RIGHT_HAND` or `FIRST_PERSON_LEFT_HAND`. It therefore controls the container transform, not the absolute transform of the item mesh or its final grip. Minecraft then applies the item's own first-person display transform, including any transform supplied by an item model or resource pack. This keeps animated items compatible with vanilla and custom first-person item models.

Keep `firstPerson` disabled for ordinary third-person animations. If an enabled definition contributes no animated hand channel, custom anchor, or mesh attachment, PulseLib leaves Minecraft's complete first-person pass intact. Definitions are processed by ascending `priority` and then identifier; arm and item transforms blend in that same order.

## Hiding first-person held items

`itemRenderPolicy(...)` supplies a predicate for each first-person item channel. It is evaluated immediately before drawing each non-empty item and receives the local player, its logical hand, and current `ItemStack`:

```java
.firstPerson(PPlayerFirstPersonSettings.ENABLED)
.itemRenderPolicy((player, hand, stack) ->
        hand == InteractionHand.MAIN_HAND && stack.is(MyItems.KATANA.get()))
```

Return `true` to suppress that item's local first-person draw. `ItemRenderPolicy.HIDE` hides an item and `ItemRenderPolicy.RENDER` keeps it visible; rendering is the default. The policy is attached to each physical left/right item channel only when that definition contributes the matching item anchor. Definitions are processed by ascending `priority` and identifier, so the highest ordered contributor for each channel owns its discrete policy while its transform is blended normally. The inventory and third-person renderer are unaffected.

## Animation anchors and mesh attachments

`anchor(PPlayerAnimationAnchor, boneName)` exposes a model bone as a named attachment point. PulseLib reserves `FIRST_PERSON_CAMERA`, `RIGHT_ITEM`, and `LEFT_ITEM`; use a custom `PPlayerAnimationAnchor` for equipment or effects that follow an animation bone. Register a `PPlayerAnimatedAttachmentRenderer` through `PulseLibEvents.PlayerAnimatedAttachmentRegistrationEvent`. Its context contains the player, animation id, anchor, sampled transform, blend weight, render stack, collector, and whether it is rendering in first person.

The player-animation model may also contain mesh branches that are not bound to a vanilla player part. PulseLib automatically renders an animated mesh branch when it has an active animated bone, both in third person and in the enabled first-person replacement pass. Its transform is blended from identity by the animation activation weight before rendering, so activation and crossfade transitions also move mesh attachments smoothly. This lets a model carry animated props or effects without writing an attachment renderer. Skeleton-only models remain valid.

`populateMolangContext(...)` can add player-specific Molang queries:

```java
.populateMolangContext((player, instance, controller, context, partialTick) ->
        context.query("is_sneaking", player.isCrouching() ? 1.0f : 0.0f))
```

As with other client-side animation state, multiplayer gameplay events are the owning mod's responsibility to synchronize. The animation definition observes only state available on that client.
