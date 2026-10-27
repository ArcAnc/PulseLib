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
import com.arcanc.pulselib.content.renderer.base.PModelPartSubmitState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;

/** Replays each node's pose for main/outline/crumbling, with invocation-local restoration. */
@Mixin(ModelPartFeatureRenderer.class)
public abstract class ModelPartFeatureRendererMixin
{
	@Shadow
	@Final
	private PoseStack poseStack;

	@WrapMethod(method = "render")
	private void pulselib$restoreFeaturePose(Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> submits,
			MultiBufferSource.BufferSource buffers, OutlineBufferSource outlines,
			MultiBufferSource.BufferSource crumbling, Operation<Void> original)
	{
		int depth = ((PoseStackAccessor)this.poseStack).pulselib$getDepth();
		PoseStack.Pose entry = this.poseStack.last().copy();
		try
		{
			original.call(submits, buffers, outlines, crumbling);
		}
		finally
		{
			while (((PoseStackAccessor)this.poseStack).pulselib$getDepth() > depth)
				this.poseStack.popPose();
			this.poseStack.last().set(entry);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
	private void pulselib$replayPart(ModelPart part, PoseStack poses, VertexConsumer consumer,
			int light, int overlay, int color, Operation<Void> original,
			@Local SubmitNodeStorage.ModelPartSubmit submit)
	{
		try (var caller = PModelPartSnapshot.capture(part))
		{
			((PModelPartSubmitState)(Object)submit).pulselib$getModelPartPose().restore();
			original.call(part, poses, consumer, light, overlay, color);
		}
	}
}
