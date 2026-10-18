# Armor and Attachments

The attachment system is for cases where you do not want to replace the whole entity renderer. A player, zombie, cow, or custom living entity can keep its normal vanilla renderer, while PulseLib draws extra animated model parts on selected `ModelPart` anchors.

This is useful for custom armor, tails, backpacks, masks, equipment on arms, or other accessories. Definitions can be tied to an item stack, registered globally for living entities, hide vanilla armor for a slot, and render first-person arm attachments.

First-person arm attachments render through an enabled player-animation first-person presentation with sampled arm and camera anchors. Register every attachment model and its material references in `IPulseClientPlugin.registerModels(PModelRegistrar)` before atlas/model reload work.

Important classes:

* [`PLivingAttachmentDefinition`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentDefinition.java)
* [`PLivingAttachments`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachments.java)
* [`PLivingAttachmentSource`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSource.java)
* [`PLivingAttachmentSources`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentSources.java)
* [`PAttachmentBinding`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentBinding.java)
* [`PAttachmentAnchor`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchor.java)
* [`PAttachmentAnchorResolvers`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java)
* [`PTransform`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PTransform.java)
* [`PHumanoidAnchors`](../common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java)
* [`PHumanoidBindings`](../common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java)
* [`PLivingAttachmentLayer`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingAttachmentLayer.java)
* [`PHumanoidAttachmentLayer`](../common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAttachmentLayer.java)

PulseLib adds attachment render layers for players and living entities automatically through [`PLibArmorHandler`](../common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/armor/PLibArmorHandler.java). Attachment definitions are contributed in `IPulseClientPlugin.registerAttachments(PAttachmentRegistrar)`.

## Player-animation anchors and meshes

The player-animation system also exposes semantic anchors through `PPlayerAnimationDefinition.Builder.anchor(...)` and additional mesh subtrees through `Builder.meshAttachment(...)` or automatic discovery. These use the evaluated animation skeleton. See [Player animations](player-animations.md#animated-anchors-and-automatic-meshes) for registration and [transform spaces](player-animations.md#transform-spaces).

For `ROOT → BODY → RIGHT_ARM → ITEM_ANCHOR`, the anchor inherits every parent transform. The native third-person stack already includes visual ROOT, so PulseLib supplies the current-root-relative child matrix and applies ROOT once. In first person, arms, items, custom anchors, automatic meshes, and persistent arm attachments use the shared camera-relative presentation. Their VIEW-space transforms do not receive another ROOT or camera transform.

An animated anchor renderer receives `PPlayerAnimatedAttachmentContext.poseStack()` already positioned at the attachment origin. Render local geometry with that stack. `context.transform()` is the full source MODEL-space socket in third person and a camera-relative VIEW-space transform in first person; applying it again to the supplied stack duplicates the transform. Automatic meshes retain their parent's hierarchy even without their own animation channel, including active rigs where only the camera animates.

Root Motion and animated camera movement are visual only. Attachments do not move the entity, its bounding box, collisions, or server position. The `PTransform` used by this page's `PAttachmentBinding` examples belongs to `com.arcanc.pulselib.util.attachments`; evaluated animation sockets use the separate `com.arcanc.pulselib.content.model.animation.PTransform` TRS record.

## Armor item example

Armor is the most direct use case: register a living attachment definition for the armor item, say which equipment slot owns it, and map PulseLib bones to humanoid anchors.

```java
@PulsePlugin
public final class ExamplePlugin implements IPulseClientPlugin
{
	private static final PModelData HELMET_MODEL = PModelData.entity(
			ResourceLocation.fromNamespaceAndPath("examplemod", "armor/example_helmet"));

	@Override
	public ResourceLocation getPluginId()
	{
		return ResourceLocation.fromNamespaceAndPath("examplemod", "attachments");
	}

	@Override
	public void registerModels(PModelRegistrar registrar)
	{
		registrar.model(HELMET_MODEL).texture("base",
				ResourceLocation.fromNamespaceAndPath("examplemod", "armor/example_helmet"));
	}

	@Override
	public void registerAttachments(PAttachmentRegistrar registrar)
	{
		registrar.registerLiving(MyItems.EXAMPLE_HELMET.get(),
				new PLivingAttachmentDefinition(
						HELMET_MODEL,
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

If the GLB is absent, the loader tries the same path with `.gltf`. This example expects material reference `base` and texture `assets/examplemod/textures/armor/example_helmet.png`; use the exact reference exported by your model.

`hideVanilla = true` makes the loader armor adapter hide the vanilla armor model for matching equipment slots. Use it when the PulseLib model fully replaces the vanilla armor layer.

## General attachment item

Attachments are not limited to armor slots. A held item can render a tail, an amulet can render on the body, or a global definition can render on an entity even without a specific item stack.

```java
registrar.registerLiving(MyItems.TAIL.get(),
		new PLivingAttachmentDefinition(
				PModelData.entity(
						ResourceLocation.fromNamespaceAndPath("examplemod", "attachment/tail")),
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

Built-in anchors from [`PHumanoidAnchors`](../common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidAnchors.java):

* `HEAD`
* `HAT`
* `BODY`
* `RIGHT_ARM`
* `LEFT_ARM`
* `RIGHT_LEG`
* `LEFT_LEG`

Use [`PHumanoidBindings`](../common/src/main/java/com/arcanc/pulselib/util/attachments/humanoid/PHumanoidBindings.java) for common bindings:

```java
PHumanoidBindings.head("helmet");
PHumanoidBindings.body("chest");
PHumanoidBindings.rightArm("right_arm");
PHumanoidBindings.leftLeg("left_leg");
```

Use `PHumanoidBindings.bind(anchor, bone, transform)` when you need offset, rotation, or scale. `PTransform.of` accepts offset, Euler rotation in degrees, and scale vectors.

## Custom anchors

Register anchors for custom vanilla `EntityModel` classes through [`PAttachmentAnchorResolvers`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PAttachmentAnchorResolvers.java):

```java
public static final PAttachmentAnchor SHELL =
		PAttachmentAnchor.of(ResourceLocation.fromNamespaceAndPath("examplemod", "shell"));

public static void registerAnchors()
{
	PAttachmentAnchorResolvers.register(MyEntityModel.class, SHELL,
			(entity, model) -> ((MyEntityModel<?>) model).shell);
}
```

## Custom mesh rendering

Each definition takes a [`PLivingMeshRenderResolver`](../common/src/main/java/com/arcanc/pulselib/util/attachments/PLivingMeshRenderResolver.java). The resolver receives the living entity, stack, baked bone, baked mesh, inherited render context, and partial tick.

```java
PLivingMeshRenderResolver resolver = (entity, stack, bone, mesh, inherited, partialTick) ->
{
	int color = stack.hasFoil() ? 0xFF80FFFF : inherited.color();
	return inherited.withColor(color);
};
```

Use `PLivingMeshRenderResolvers.defaultLit()` or `inherited()` when you do not need per-mesh overrides.

## Animated attachments

`PLivingAttachmentDefinition.ControllerProvider` supplies controllers for each render sample. For an attachment backed by an animatable item, reuse its stack-specific manager:

```java
PLivingAttachmentDefinition.ControllerProvider provider = (entity, stack, model, partialTick) ->
{
	if (stack.isEmpty() || !(stack.getItem() instanceof PAnimatable<?>))
	{
		return List.of();
	}
	PAnimationManager<?> manager = PLibHelper.getManager(stack);
	manager.bindModel(model);
	return List.copyOf(manager.getControllers().values());
};
```

Pass the provider as the definition constructor's final argument. Its item must register controllers using animation names present in the attachment model. This shares the item's keyed timeline, so use a dedicated manager if the item renderer needs an incompatible model.

Attachments without a `PAnimatable` can supply standalone controllers instead. Retain them per attachment owner, tick once per game tick with the real elapsed time, and release them when the owner disappears. Creating or ticking a fresh controller in this render callback resets playback or makes its speed depend on frame rate.
