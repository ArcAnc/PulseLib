/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric.mixin;

import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelProvider;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin (HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin
{
	@WrapMethod (method = "renderArmorPiece")
	private void pulselib$armorModel(PoseStack poses, MultiBufferSource buffers, LivingEntity entity,
	                                 EquipmentSlot slot, int light, HumanoidModel<?> model, Operation<Void> original)
	{
		var stack = entity.getItemBySlot(slot);
		if (stack.getItem() instanceof PArmorModelProvider provider)
			model = Objects.requireNonNull(provider.getPulseArmorModel(entity, stack, slot, model), "Pulse armor model");
		original.call(poses, buffers, entity, slot, light, model);
	}

	@Inject (method = "renderArmorPiece", at = @At ("HEAD"), cancellable = true)
	private void pulselib$hideVanillaArmor(PoseStack poses, MultiBufferSource buffers, LivingEntity entity,
	                                       EquipmentSlot slot, int light, HumanoidModel<?> model, CallbackInfo callback)
	{
		if (PLivingAttachments.hidesVanillaArmor(entity.getItemBySlot(slot), slot, entity))
			callback.cancel();
	}

	@Inject (method = "renderArmorPiece", at = @At (value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V", shift = At.Shift.AFTER))
	private void pulselib$deformArmor(PoseStack poses, MultiBufferSource buffers, LivingEntity entity,
	                                  EquipmentSlot slot, int light, HumanoidModel<?> model, CallbackInfo callback)
	{
		if (entity.getItemBySlot(slot).getItem() instanceof PArmorModelProvider provider)
			provider.setupPulseArmorAnimations(entity, entity.getItemBySlot(slot), slot, model, Minecraft.getInstance().
					getTimer().getGameTimeDeltaPartialTick(false));
		if (entity instanceof Player player)
			PPlayerMeshDeformers.apply(player, model, Minecraft.getInstance().getTimer().
					getGameTimeDeltaPartialTick(false));
	}
}
