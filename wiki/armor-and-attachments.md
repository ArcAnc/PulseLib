# Armor and Attachments

The attachment system is for cases where you do not want to replace the whole entity renderer. A player, zombie, cow, or custom living entity can keep its normal vanilla renderer, while PulseLib draws extra animated model parts on selected `ModelPart` anchors.

This is useful for custom armor, tails, backpacks, masks, equipment on arms, or other accessories. Definitions can be tied to an item stack, registered globally for living entities, hide vanilla armor for a slot, and render first-person arm attachments.

First-person arm attachments are rendered only by PulseLib's opt-in player-animation first-person pass. The local player needs an active definition with `firstPerson(PPlayerFirstPersonSettings.ENABLED)`, a `FIRST_PERSON_CAMERA` anchor, and a sampled transform for that arm. See [Player animations](player-animations.md#first-person-hands-items-and-camera-space) when an attachment must appear in first person.

Important classes:

* [`PLivingAttachmentDefinition`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentDefinition.java)
* [`PLivingAttachments`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachments.java)
* [`PLivingAttachmentSource`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSource.java)
* [`PLivingAttachmentSources`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSources.java)
* [`PAttachmentBinding`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentBinding.java)
* [`PAttachmentAnchor`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchor.java)
* [`PAttachmentAnchorResolvers`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java)
* [`PTransform`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PTransform.java)
* [`PHumanoidAnchors`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java)
* [`PHumanoidBindings`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java)
* [`PLivingAttachmentLayer`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentLayer.java)
* [`PHumanoidAttachmentLayer`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAttachmentLayer.java)
* [`PArmorClientExtensions`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/neoforge/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PArmorClientExtensions.java)

PulseLib adds attachment render layers for players and living entities automatically through [`PLibArmorHandler`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java). Attachment definitions are contributed through [`IPulseClientPlugin.registerAttachments(PAttachmentRegistrar)`](plugins.md).

Before registering an attachment definition, register its model and material texture references in `IPulseClientPlugin.registerModels(PModelRegistrar)`; otherwise that model is not loaded. See [Resources](resources.md#register-model-resources).

## Armor item example

Armor is the most direct use case: register a living attachment definition for the armor item, say which equipment slot owns it, and map PulseLib bones to humanoid anchors.

```java
@PulsePlugin
public final class ExamplePulsePlugin implements IPulseClientPlugin
{
	@Override
	public Identifier getPluginId()
	{
		return Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "attachments");
	}

	@Override
	public void registerAttachments(PAttachmentRegistrar registrar)
	{
		registrar.registerLiving(MyItems.EXAMPLE_HELMET.get(),
				new PLivingAttachmentDefinition(
						PModelData.entity(
								Identifier.fromNamespaceAndPath("examplemod", "armor/example_helmet")),
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

`hideVanilla = true` suppresses matching native armor geometry while the PulseLib attachment supplies its replacement. Shared extraction hooks select armor models on both loaders. `PArmorClientExtensions` is a NeoForge-only adapter; Common/Fabric code must not depend on that class.

## General attachment item

Attachments are not limited to armor slots. A held item can render a tail, an amulet can render on the body, or a global definition can render on an entity even without a specific item stack.

```java
registrar.registerLiving(MyItems.TAIL.get(),
		new PLivingAttachmentDefinition(
				PModelData.entity(
						Identifier.fromNamespaceAndPath("examplemod", "attachment/tail")),
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
registrar.registerGlobalLiving(new PLivingAttachmentDefinition(
		MODEL_DATA,
		PLivingAttachmentSources.entityPredicate(entity -> entity.isInvisible()),
		List.of(PHumanoidBindings.body("aura")),
		PLivingMeshRenderResolvers.defaultLit(),
		false));
```

## Humanoid anchors

Built-in anchors from [`PHumanoidAnchors`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java):

* `HEAD`
* `HAT`
* `BODY`
* `RIGHT_ARM`
* `LEFT_ARM`
* `RIGHT_LEG`
* `LEFT_LEG`

Use [`PHumanoidBindings`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java) for common bindings:

```java
PHumanoidBindings.head("helmet");
PHumanoidBindings.body("chest");
PHumanoidBindings.rightArm("right_arm");
PHumanoidBindings.leftLeg("left_leg");
```

Use `PHumanoidBindings.bind(anchor, bone, transform)` when you need offset, rotation, or scale. `PTransform.of` accepts offset, Euler rotation in degrees, and scale vectors.

## Custom anchors

Register anchors for custom vanilla `EntityModel` classes through [`PAttachmentAnchorResolvers`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java):

```java
public static final PAttachmentAnchor SHELL =
		PAttachmentAnchor.of(Identifier.fromNamespaceAndPath("examplemod", "shell"));

public static void registerAnchors()
{
	PAttachmentAnchorResolvers.register(MyEntityModel.class, SHELL,
			(entity, model) -> ((MyEntityModel<?>) model).shell);
}
```

## Custom mesh rendering

Each definition takes a [`PLivingMeshRenderResolver`](https://github.com/ArcAnc/PulseLib/blob/26.1.2-multiloader/common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolver.java). The resolver receives the living entity, stack, baked bone, baked mesh, inherited render context, and partial tick.

```java
PLivingMeshRenderResolver resolver = (entity, stack, bone, mesh, inherited, partialTick) ->
{
	int color = stack.hasFoil() ? 0xFF80FFFF : inherited.color();
	return inherited.withColor(color);
};
```

Use the context's `with...` methods so texture, emissive, alpha-mode, and deformation overrides from other resolvers are retained. Use `PLivingMeshRenderResolvers.defaultLit()` or `inherited()` when you do not need per-mesh overrides.

## Animated attachments

`PLivingAttachmentDefinition` can supply standalone controllers. This is useful when the attachment is not itself an item/entity/block entity implementing `PAnimatable`.

```java
PLivingAttachmentDefinition.ControllerProvider provider = (entity, stack, model, partialTick) ->
{
	PAnimationController<MyAttachmentAnimatable> controller =
			new PAnimationController<>("idle", state ->
			{
				state.controller().play(IDLE);
				return ControllerState.PLAY;
			});

	controller.tick(MY_ATTACHMENT_ANIMATABLE, 1, model);
	return List.of(controller);
};
```

Pass that provider as the sixth `PLivingAttachmentDefinition` constructor argument. The five-argument constructor uses `ControllerProvider.EMPTY`, so it provides no independent clips.

The provider above illustrates sampling a controller; creating it on every extraction would restart playback. Cache real controllers per entity/stack and tick them once through their owning client lifecycle, then return those controllers from the provider. The attachment layer evaluates their own clip pose and composes it with the selected vanilla anchor. Bindings, texture mappings and model-resource registration are still required.

## Native extraction and armor providers

PulseLib installs compatible living/player layers on both loaders and extracts attachment entries into the native living render state. Bone matrices and render inputs are captured when queued; later controller mutations do not change matrices already submitted. Do not treat the returned controller collection as an immutable extraction snapshot or manually invoke a second automatic attachment pass.

`PArmorModelProvider` remains the shared customization point for armor models. On NeoForge, automatic client-extension registration preserves an item's existing external extension. When a mod explicitly composes its own extension, use `PArmorClientExtensions.buildFor(item, base)` to preserve unrelated font, arm, overlay, tint and other native callbacks. Use the shared provider for PulseLib model selection; native equipment queries now use `EquipmentClientInfo.LayerType`.

The old BEWLR `getCustomRenderer()` integration is retired. The legacy dimension-only `renderHelmetOverlay(...)` overload is a deprecated no-op; implement the native `renderFirstPersonOverlay` callback with `GuiGraphicsExtractor` in a NeoForge extension when an overlay is required.
