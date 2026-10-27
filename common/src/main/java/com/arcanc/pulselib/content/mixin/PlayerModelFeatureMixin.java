/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Restores the shared player model after all native main/outline/crumbling deferred draws. */
@Mixin(ModelFeatureRenderer.class)
public abstract class PlayerModelFeatureMixin
{
	@Shadow
	@Final
	private PoseStack poseStack;

	@WrapMethod(method = "renderModel")
	private void pulselib$restoreDeferredPlayer(SubmitNodeStorage.ModelSubmit<?> submit, RenderType type,
			VertexConsumer buffer, OutlineBufferSource outlines, MultiBufferSource.BufferSource crumbling,
			Operation<Void> original)
	{
		if (!(submit.model() instanceof PlayerModel model) || !(submit.state() instanceof AvatarRenderState))
		{
			original.call(submit, type, buffer, outlines, crumbling);
			return;
		}
		int depth = ((PoseStackAccessor)this.poseStack).pulselib$getDepth();
		PoseStack.Pose entry = this.poseStack.last().copy();
		try (var snapshot = PModelPartSnapshot.capture(model.root()))
		{
			original.call(submit, type, buffer, outlines, crumbling);
		}
		finally
		{
			while (((PoseStackAccessor)this.poseStack).pulselib$getDepth() > depth)
				this.poseStack.popPose();
			this.poseStack.last().set(entry);
		}
	}
}
