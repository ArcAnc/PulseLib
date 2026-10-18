/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.mojang.blaze3d.vertex.PoseStack;
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

/**
 * Integrates PulseLib armor animation, attachment replacement and player deformation with the loader armor
 * render path.
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin
{
	/**
	 * Applies player deformation after NeoForge has configured armor model animation.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param buffers the vanilla vertex buffer source
	 * @param entity the entity being animated or rendered
	 * @param slot the equipment slot being inspected
	 * @param packedLight the packed block and sky light coordinates
	 * @param model the model
	 * @param limbSwing the entity limb animation position
	 * @param limbSwingAmount the entity limb animation amplitude
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param ageInTicks the entity age used by vanilla render animation
	 * @param netHeadYaw the head yaw relative to the body in degrees
	 * @param headPitch the head pitch in degrees
	 * @param callback the Mixin callback used to inspect or replace the intercepted result
	 */
	@Inject(
			method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
			at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;setupModelAnimations(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/client/model/Model;FFFFFF)V", shift = At.Shift.AFTER))
	private void pulselib$applyPlayerDeformersToArmor(PoseStack poseStack,
	                                                  MultiBufferSource buffers,
	                                                  LivingEntity entity,
	                                                  EquipmentSlot slot,
	                                                  int packedLight,
	                                                  HumanoidModel<?> model,
	                                                  float limbSwing,
	                                                  float limbSwingAmount,
	                                                  float partialTick,
	                                                  float ageInTicks,
	                                                  float netHeadYaw,
	                                                  float headPitch,
	                                                  CallbackInfo callback)
	{
		if (entity instanceof Player player)
			PPlayerMeshDeformers.apply(player, model, partialTick);
	}
}
