# Armor and Attachments

The attachment system is for cases where you do not want to replace the whole entity renderer. A player, zombie, cow, or custom living entity can keep its normal vanilla renderer, while PulseLib draws extra animated model parts on selected `ModelPart` anchors.

This is useful for custom armor, tails, backpacks, masks, equipment on arms, or other accessories. Definitions can be tied to an item stack, registered globally for living entities, hide vanilla armor for a slot, and render first-person arm attachments.

First-person arm attachments are rendered only by PulseLib's opt-in player-animation first-person pass. The local player needs an active definition with `firstPerson(PPlayerFirstPersonSettings.ENABLED)`, a `FIRST_PERSON_CAMERA` anchor, and a sampled transform for that arm. See [Player animations](player-animations.md#first-person-hands-items-and-camera-space) when an attachment must appear in first person.

Important classes:

* [`PLivingAttachmentDefinition`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentDefinition.java)
* [`PLivingAttachments`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachments.java)
* [`PLivingAttachmentSource`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSource.java)
* [`PLivingAttachmentSources`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSources.java)
* [`PAttachmentBinding`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentBinding.java)
* [`PAttachmentAnchor`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchor.java)
* [`PAttachmentAnchorResolvers`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java)
* [`PTransform`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PTransform.java)
* [`PHumanoidAnchors`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java)
* [`PHumanoidBindings`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java)
* [`PLivingAttachmentLayer`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentLayer.java)
* [`PHumanoidAttachmentLayer`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAttachmentLayer.java)
* [`PArmorClientExtensions`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java)

PulseLib adds attachment render layers for players and living entities automatically through [`PLibArmorHandler`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java). Attachment definitions should be contributed on the mod event bus through [`PulseLibEvents.AttachmentRegistrationEvent`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/event/PulseLibEvents.java).

## Armor item example

Armor is the most direct use case: register a living attachment definition for the armor item, say which equipment slot owns it, and map PulseLib bones to humanoid anchors.

```java
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExampleClientEvents {
    @SubscribeEvent
    public static void registerAttachments(PulseLibEvents.AttachmentRegistrationEvent event) {
        event.registration().registerLiving(MyItems.EXAMPLE_HELMET.get(),
                new PLivingAttachmentDefinition(
                        new PModelData.Builder(
                                Identifier.fromNamespaceAndPath("examplemod", "armor/example_helmet"),
                                "entity").build(),
                        PLivingAttachmentSources.equipmentSlot(EquipmentSlot.HEAD),
                        List.of(PHumanoidBindings.head("helmet")),
                        PLivingMeshRenderResolvers.defaultLit(),
                        true));
    }
}
```

For the default glTF loader this model path resolves to:

```text
assets/examplemod/glmodels/entity/armor/example_helmet.glb
```

`hideVanilla = true` makes `PArmorClientExtensions` hide the vanilla armor model for matching equipment slots. Use it when the PulseLib model fully replaces the vanilla armor layer.

## General attachment item

Attachments are not limited to armor slots. A held item can render a tail, an amulet can render on the body, or a global definition can render on an entity even without a specific item stack.

```java
event.registration().registerLiving(MyItems.TAIL.get(),
        new PLivingAttachmentDefinition(
                new PModelData.Builder(
                        Identifier.fromNamespaceAndPath("examplemod", "attachment/tail"),
                        "entity").build(),
                PLivingAttachmentSources.hand(),
                List.of(PHumanoidBindings.bind(
                        PHumanoidAnchors.BODY,
                        "tail",
                        PTransform.of(
                                new Vector3f(0.0f, -0.2f, 0.4f),
                                new Vector3f(90.0f, 0.0f, 0.0f),
                                new Vector3f(1.0f, 1.0f, 1.0f)))),
                PLivingMeshRenderResolvers.defaultLit(),
                false));
```

`PLivingAttachmentSource` controls when a definition renders. Built-in factories:

```java
PLivingAttachmentSources.anyEquipmentSlot();
PLivingAttachmentSources.equipmentSlot(EquipmentSlot.HEAD);
PLivingAttachmentSources.hand();
PLivingAttachmentSources.entityPredicate(entity -> entity.isCrouching());
PLivingAttachmentSources.equipmentSlotPredicate(EquipmentSlot.CHEST, entity -> entity.isAlive());
```

Global attachments use the same definition type:

```java
event.registration().registerGlobalLiving(new PLivingAttachmentDefinition(
        MODEL_DATA,
        PLivingAttachmentSources.entityPredicate(entity -> entity.isInvisible()),
        List.of(PHumanoidBindings.body("aura")),
        PLivingMeshRenderResolvers.defaultLit(),
        false));
```

## Humanoid anchors

Built-in anchors from [`PHumanoidAnchors`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java):

* `HEAD`
* `HAT`
* `BODY`
* `RIGHT_ARM`
* `LEFT_ARM`
* `RIGHT_LEG`
* `LEFT_LEG`

Use [`PHumanoidBindings`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java) for common bindings:

```java
PHumanoidBindings.head("helmet");
PHumanoidBindings.body("chest");
PHumanoidBindings.rightArm("right_arm");
PHumanoidBindings.leftLeg("left_leg");
```

Use `PHumanoidBindings.bind(anchor, bone, transform)` when you need offset, rotation, or scale. `PTransform.of` accepts offset, Euler rotation in degrees, and scale vectors.

## Custom anchors

Register anchors for custom vanilla `EntityModel` classes through [`PAttachmentAnchorResolvers`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java):

```java
public static final PAttachmentAnchor SHELL =
        PAttachmentAnchor.of(Identifier.fromNamespaceAndPath("examplemod", "shell"));

public static void registerAnchors() {
    PAttachmentAnchorResolvers.register(MyEntityModel.class, SHELL,
            (entity, model) -> ((MyEntityModel<?>) model).shell);
}
```

## Custom mesh rendering

Each definition takes a [`PLivingMeshRenderResolver`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolver.java). The resolver receives the living entity, stack, baked bone, baked mesh, inherited render context, and partial tick.

```java
PLivingMeshRenderResolver resolver = (entity, stack, bone, mesh, inherited, partialTick) -> {
    int color = stack.hasFoil() ? 0xFF80FFFF : inherited.color();
    return inherited.withColor(color);
};
```

Use the context's `with...` methods so texture, emissive, alpha-mode, and deformation overrides from other resolvers are retained. Use `PLivingMeshRenderResolvers.defaultLit()` or `inherited()` when you do not need per-mesh overrides.

## Animated attachments

`PLivingAttachmentDefinition` can supply standalone controllers. This is useful when the attachment is not itself an item/entity/block entity implementing `PAnimatable`.

```java
PLivingAttachmentDefinition.ControllerProvider provider = (entity, stack, model, partialTick) -> {
    PAnimationController<MyAttachmentAnimatable> controller =
            new PAnimationController<>("idle", state -> {
                state.controller().play(IDLE);
                return ControllerState.PLAY;
            });

    controller.tick(MY_ATTACHMENT_ANIMATABLE, 1, model);
    return List.of(controller);
};
```

For real items, cache controllers per entity or stack instead of creating a new controller every frame.
