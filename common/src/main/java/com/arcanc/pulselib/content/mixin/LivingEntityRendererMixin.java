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

/**
 * Applies player pose animation and deformation around vanilla living-entity rendering.
 *
 * @param <T> the entity type rendered or animated by this contract
 * @param <M> the entity type rendered or animated by this contract
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>>
{
	@Unique private @Nullable PPlayerAnimations.PPlayerModelPose pulselib$playerPose;

	/**
	 * Vanilla living-entity model temporarily modified during player animation rendering.
	 */
	@Shadow protected M model;

	/**
	 * Applies player animation and mesh deformation before the living model renders.
	 *
	 * @param entity the entity being animated or rendered
	 * @param entityYaw the entity yaw in degrees
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param poseStack the current model-to-render pose stack
	 * @param bufferSource the vanilla vertex buffer source
	 * @param packedLight the packed block and sky light coordinates
	 * @param ci the Mixin callback for the intercepted method
	 */
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

	/**
	 * Restores vanilla model-part transforms after living entity rendering.
	 *
	 * @param entity the entity being animated or rendered
	 * @param entityYaw the entity yaw in degrees
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param poseStack the current model-to-render pose stack
	 * @param bufferSource the vanilla vertex buffer source
	 * @param packedLight the packed block and sky light coordinates
	 * @param ci the Mixin callback for the intercepted method
	 */
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
