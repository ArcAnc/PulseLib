/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelSnapshots;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Replays provider poses after native setup, with no live-owner callbacks and no shared-model leakage. */
@Mixin(value = ModelFeatureRenderer.class, priority = 900)
public abstract class ArmorProviderFeatureMixin
{
	@Shadow
	@Final
	private PoseStack poseStack;

	@WrapMethod(method = "renderModel")
	private void pulselib$restoreProvider(SubmitNodeStorage.ModelSubmit<?> submit, RenderType type,
			VertexConsumer buffer, OutlineBufferSource outlines, MultiBufferSource.BufferSource crumbling, Operation<Void> original)
	{
		if (((PArmorModelSnapshots.Node)(Object)submit).pulselib$getArmorProvider() == null)
		{
			original.call(submit, type, buffer, outlines, crumbling);
			return;
		}
		int depth = ((PoseStackAccessor)this.poseStack).pulselib$getDepth();
		var pose = this.poseStack.last().copy();
		try (var caller = PModelPartSnapshot.capture(submit.model().root()))
		{
			original.call(submit, type, buffer, outlines, crumbling);
		}
		finally
		{
			while (((PoseStackAccessor)this.poseStack).pulselib$getDepth() > depth)
				this.poseStack.popPose();
			this.poseStack.last().set(pose);
		}
	}

	@WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"))
	private void pulselib$replayProvider(Model<?> model, Object state, Operation<Void> original,
			@Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> submit)
	{
		original.call(model, state);
		var entry = ((PArmorModelSnapshots.Node)(Object)submit).pulselib$getArmorProvider();
		if (entry != null)
			entry.snapshot().restore();
	}
}
