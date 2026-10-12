/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.deformer.PPlayerMeshDeformers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>>
{
	@Unique private @Nullable PPlayerAnimations.PPlayerModelPose pulselib$playerPose;

	@Shadow protected M model;

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
					shift = At.Shift.AFTER))
	private void pulselib$applyPlayerAnimations(T entity,
	                                             float entityYaw,
	                                             float partialTick,
	                                             PoseStack poseStack,
	                                             MultiBufferSource bufferSource,
	                                             int packedLight,
	                                             CallbackInfo ci)
	{
		if (!(entity instanceof Player player) || !(this.model instanceof PlayerModel<?> playerModel))
			return;

		this.pulselib$playerPose = PPlayerAnimations.apply(
				player,
				playerModel,
				partialTick,
				PPlayerAnimations.allParts());
		PPlayerMeshDeformers.apply(player, playerModel, partialTick);
		PPlayerAnimations.applyRoot(player, poseStack, partialTick);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void pulselib$restorePlayerAnimations(T entity,
	                                               float entityYaw,
	                                               float partialTick,
	                                               PoseStack poseStack,
	                                               MultiBufferSource bufferSource,
	                                               int packedLight,
	                                               CallbackInfo ci)
	{
		if (!(entity instanceof Player) || !(this.model instanceof PlayerModel<?>))
			return;

		if (this.pulselib$playerPose != null)
		{
			this.pulselib$playerPose.restore();
			this.pulselib$playerPose = null;
		}
	}
}
