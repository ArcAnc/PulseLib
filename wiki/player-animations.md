# Player animations

## First-person integration (Minecraft 1.21.1)

First-person output is opt-in with `firstPerson(PPlayerFirstPersonSettings.ENABLED)` and requires the `FIRST_PERSON_CAMERA` anchor. Bind `RIGHT_ARM` and `LEFT_ARM` for the physical arms, and use `RIGHT_ITEM` and `LEFT_ITEM` anchors for held items. PulseLib preserves the native 1.21.1 `ItemInHandRenderer` equip, swing, use, map, display-context, and left/right hand pipeline; it replaces only the final arm or item spatial submission when the presentation supplies an animated pose.

`PFirstPersonRenderContexts` carries a per-hand presentation through the renderer. `PVanillaFirstPersonArmResolver` and `PVanillaFirstPersonItemResolver` adapt camera-relative animation transforms to the vanilla arm and item origins. `PPlayerFirstPersonRenderer` draws first-person anchor and mesh attachments before the native buffer batch closes. `PFirstPersonCameraMode.ANIMATED` applies the `FIRST_PERSON_CAMERA` delta in `Camera.setup`; `VANILLA` leaves the camera unchanged.

Register custom first-person anchor renderers on `PulseLibEvents.PlayerAnimatedAttachmentRegistrationEvent`. The callback receives the animated transform, its weight, `PoseStack`, `MultiBufferSource`, and a `firstPerson` flag.

Player animations modify the vanilla player model after Minecraft has prepared its normal walk, swim, crouch, and item-use pose. They work for both classic and slim skins, including hats, jackets, sleeves, and pants layers.

The API is client-only. Register definitions on the mod event bus through `PulseLibEvents.PlayerAnimationRegistrationEvent`:

```java
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ExamplePlayerAnimations {
    private static final PModelData COMBAT_MODEL = new PModelData.Builder(
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "player/combat"),
            "player",
            PGeckoModelLoader.INSTANCE.id()).build();

    private static final PRawAnimation IDLE = PRawAnimation.begin()
            .thenLoop("animation.combat.idle")
            .build();

    @SubscribeEvent
    public static void register(PulseLibEvents.PlayerAnimationRegistrationEvent event) {
        event.registration().register(
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "combat_pose"),
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

`ROOT` applies the bone transform to the complete third-person player render, including its feature layers. Use it for an emote that moves or rotates the whole player, such as a flip. `rootPivot(x, y, z)` selects its rotation pivot in model-space blocks; a value close to `(0, 0.9, 0)` rotates around the centre of a standing player. In first person, `ROOT` and `HEAD` drive the local camera, while arms use only their local arm transforms. The root transform is deliberately not applied a second time to the first-person hand renderer. An empty off-hand is rendered when its `LEFT_ARM` or `RIGHT_ARM` binding has an active sampled transform.

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

Use this for layered models whose walk, idle, or other cyclic animations must stay aligned. `boneWeight(boneName, weight)` can additionally scale one bound bone independently of its semantic player-part weight.

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

The API affects the whole player model in third person and arms in first person. It restores position, rotation, and scale immediately after every draw, preventing a pose from leaking into a different player or another render layer.

`populateMolangContext(...)` can add player-specific Molang queries:

```java
.populateMolangContext((player, instance, controller, context, partialTick) ->
        context.query("is_sneaking", player.isCrouching() ? 1.0f : 0.0f))
```

As with other client-side animation state, multiplayer gameplay events are the owning mod's responsibility to synchronize. The animation definition observes only state available on that client.
