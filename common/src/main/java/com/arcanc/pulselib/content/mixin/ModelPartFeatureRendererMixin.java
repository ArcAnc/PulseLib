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
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Replays each node's pose for main/outline/crumbling, with invocation-local restoration. */
@Mixin(ModelFeatureRenderer.class)
public abstract class ModelPartFeatureRendererMixin
{
	@Shadow
	@Final
	private PoseStack poseStack;

	@WrapMethod(method = "prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V")
	private void pulselib$restoreFeaturePose(ModelFeatureRenderer.Submit<?> submit, Operation<Void> original)
	{
		if (!(submit.model() instanceof Model.Simple))
		{
			original.call(submit);
			return;
		}
		int depth = ((PoseStackAccessor)this.poseStack).pulselib$getDepth();
		PoseStack.Pose entry = this.poseStack.last().copy();
		try (var caller = PModelPartSnapshot.capture(submit.model().root()))
		{
			original.call(submit);
		}
		finally
		{
			while (((PoseStackAccessor)this.poseStack).pulselib$getDepth() > depth)
				this.poseStack.popPose();
			this.poseStack.last().set(entry);
		}
	}

	@WrapOperation(method = "prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
	private void pulselib$replayPart(Model<?> model, PoseStack poses, VertexConsumer consumer,
			int light, int overlay, int color, Operation<Void> original,
			@Local(argsOnly = true) ModelFeatureRenderer.Submit<?> submit)
	{
		if (model instanceof Model.Simple)
			((PModelPartSubmitState)(Object)submit).pulselib$getModelPartPose().restore();
		original.call(model, poses, consumer, light, overlay, color);
	}
}
