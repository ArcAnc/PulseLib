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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin
{
	/**
	 * Returns the armor model.
	 * @param state the state to use.
	 * @param slot the slot to use.
	 * @return the value produced by this operation.
	 */
	@Shadow
	protected abstract HumanoidModel<?> getArmorModel(HumanoidRenderState state, EquipmentSlot slot);

	/**
	 * Performs the pulselib$apply player deformers to armor operation.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param itemStack the item stack to use.
	 * @param slot the slot to use.
	 * @param packedLight the packed light to use.
	 * @param state the state to use.
	 * @param callback the callback to use.
	 */
	@Inject(method = "renderArmorPiece", at = @At("HEAD"))
	private void pulselib$applyPlayerDeformersToArmor(PoseStack poseStack,
	                                                  SubmitNodeCollector submitNodeCollector,
	                                                  ItemStack itemStack,
	                                                  EquipmentSlot slot,
	                                                  int packedLight,
	                                                  HumanoidRenderState state,
	                                                  CallbackInfo callback)
	{
		if (!(state instanceof AvatarRenderState avatar) || Minecraft.getInstance().level == null ||
				!(Minecraft.getInstance().level.getEntity(avatar.id) instanceof Player player))
			return;
		PPlayerMeshDeformers.apply(player, this.getArmorModel(state, slot), state.partialTick);
	}
}
