/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.content.player.deformer.PPlayerModelGeometry;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.renderer.base.PRenderCallbackScope;
import com.arcanc.pulselib.platform.PClientRenderHooks;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelProvider;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelSnapshots;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Evaluates the canonical provider once during extraction; native draws replay its frozen result. */
@Mixin(value = HumanoidArmorLayer.class, priority = 900)
public abstract class ArmorProviderLayerMixin implements PArmorModelSnapshots.Layer
{
	@Unique
	private final Object pulselib$providerKey = new Object();

	@Shadow
	protected abstract HumanoidModel<?> getArmorModel(HumanoidRenderState state, EquipmentSlot slot);

	@Override
	public void pulselib$captureArmorProviders(LivingEntity owner, HumanoidRenderState state, float partialTick,
			Map<Object, Map<EquipmentSlot, PArmorModelSnapshots.Entry>> entries)
	{
		Map<EquipmentSlot, PArmorModelSnapshots.Entry> slots = new EnumMap<>(EquipmentSlot.class);
		for (EquipmentSlot slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD))
		{
			ItemStack stack = switch (slot)
			{
				case CHEST -> state.chestEquipment;
				case LEGS -> state.legsEquipment;
				case FEET -> state.feetEquipment;
				case HEAD -> state.headEquipment;
				default -> ItemStack.EMPTY;
			};
			if (!(stack.getItem() instanceof PArmorModelProvider provider) || !HumanoidArmorLayer.shouldRender(stack, slot) || PLivingAttachments.hidesVanillaArmor(stack, slot, owner))
				continue;
			var original = this.getArmorModel(state, slot);
			var type = state.isBaby && state.entityType != EntityType.ARMOR_STAND ? EquipmentClientInfo.LayerType.HUMANOID_BABY :
					slot == EquipmentSlot.LEGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
			try (var caller = PModelPartSnapshot.capture(original.root()))
			{
				pulselib$setup(original, state);
				var base = PClientRenderHooks.armorModel(stack, type, original);
				var humanoid = base instanceof HumanoidModel<?> model ? model : original;
				try (var baseCaller = PModelPartSnapshot.capture(humanoid.root()))
				{
					var selected = Objects.requireNonNull(provider.getPulseArmorModel(owner, stack, slot, humanoid), "Pulse armor model");
					try (var selectedCaller = PModelPartSnapshot.capture(selected.root()))
					{
						pulselib$setup(selected, state);
						pulselib$visibility(selected, slot);
						provider.setupPulseArmorAnimations(owner, stack, slot, selected, partialTick);
						if (owner instanceof Player player)
						{
							if (selected instanceof PlayerModel playerModel)
								PPlayerMeshDeformers.apply(player, playerModel, partialTick);
							else
								PPlayerMeshDeformers.apply(player, selected, partialTick);
						}
						slots.put(slot, new PArmorModelSnapshots.Entry(selected, PModelPartSnapshot.captureForSubmit(selected.root())));
					}
				}
			}
		}
		if (!slots.isEmpty())
			entries.put(this.pulselib$providerKey, Map.copyOf(slots));
	}

	/** MC26 bakes slot-specific vanilla meshes; full provider models still need the canonical slot mask. */
	@Unique
	private static void pulselib$visibility(HumanoidModel<?> model, EquipmentSlot slot)
	{
		model.head.visible = model.hat.visible = slot == EquipmentSlot.HEAD;
		model.body.visible = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
		model.leftArm.visible = model.rightArm.visible = slot == EquipmentSlot.CHEST;
		model.leftLeg.visible = model.rightLeg.visible = slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Unique
	private static void pulselib$setup(HumanoidModel<?> model, HumanoidRenderState state)
	{
		if (state instanceof AvatarRenderState avatar)
			PPlayerModelGeometry.setup(model, avatar);
		else
			((Model)model).setupAnim(state);
	}

	@WrapMethod(method = "renderArmorPiece")
	private void pulselib$submitProvider(PoseStack poses, SubmitNodeCollector collector, ItemStack stack,
			EquipmentSlot slot, int light, HumanoidRenderState state, Operation<Void> original)
	{
		if (!(stack.getItem() instanceof PArmorModelProvider))
		{
			original.call(poses, collector, stack, slot, light, state);
			return;
		}
		var access = (PArmorModelSnapshots.State)state;
		var slots = access.pulselib$getArmorProviders().get(this.pulselib$providerKey);
		var entry = slots == null ? null : slots.get(slot);
		if (entry == null)
			return;
		var previous = access.pulselib$getSubmittingArmorProvider();
		access.pulselib$setSubmittingArmorProvider(entry);
		try (var caller = PRenderCallbackScope.capture(poses, entry.model().root()))
		{
			original.call(poses, collector, stack, slot, light, state);
		}
		finally
		{
			access.pulselib$setSubmittingArmorProvider(previous);
		}
	}

	@WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;getArmorModel(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/client/model/HumanoidModel;"))
	private HumanoidModel<?> pulselib$selectedModel(HumanoidArmorLayer<?, ?, ?> layer, HumanoidRenderState state,
			EquipmentSlot slot, Operation<HumanoidModel<?>> original)
	{
		var entry = PArmorModelSnapshots.submitting(state);
		return entry == null ? original.call(layer, state, slot) : entry.model();
	}
}
