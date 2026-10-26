/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.base.PPlayerRootRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Applies extracted visual ROOT inside vanilla's pose and restores each invocation independently.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRootTransformMixin
{
	@WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
	private void pulselib$playerInvocation(LivingEntityRenderState state, PoseStack poses,
										   SubmitNodeCollector collector, CameraRenderState camera,
										   Operation<Void> original)
	{
		if (!(state instanceof AvatarRenderState))
		{
			original.call(state, poses, collector, camera);
			return;
		}
		int depth = ((PoseStackAccessor)poses).pulselib$getDepth();
		PoseStack.Pose entry = poses.last().copy();
		try
		{
			original.call(state, poses, collector, camera);
		}
		finally
		{
			while (((PoseStackAccessor)poses).pulselib$getDepth() > depth)
				poses.popPose();
			poses.last().set(entry);
		}
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE",
																																																																		   target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 1))
	private void
	pulselib$applyExtractedRoot(PoseStack poses, float x, float y, float z,
								Operation<Void> original,
								@Local(argsOnly = true) LivingEntityRenderState state)
	{
		original.call(poses, x, y, z);
		if (state instanceof AvatarRenderState && state instanceof PPlayerRootRenderState snapshot)
		{
			@Nullable
			Matrix4f root = snapshot.pulselib$getPlayerRoot();
			if (root != null)
				poses.mulPose(root);
		}
	}
}
