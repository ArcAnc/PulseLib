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
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
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

	@WrapMethod(method = "prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V")
	private void pulselib$restoreProvider(ModelFeatureRenderer.Submit<?> submit, Operation<Void> original)
	{
		if (((PArmorModelSnapshots.Node)(Object)submit).pulselib$getArmorProvider() == null)
		{
			original.call(submit);
			return;
		}
		int depth = ((PoseStackAccessor)this.poseStack).pulselib$getDepth();
		var pose = this.poseStack.last().copy();
		try (var caller = PModelPartSnapshot.capture(submit.model().root()))
		{
			original.call(submit);
		}
		finally
		{
			while (((PoseStackAccessor)this.poseStack).pulselib$getDepth() > depth)
				this.poseStack.popPose();
			this.poseStack.last().set(pose);
		}
	}

	@WrapOperation(method = "prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"))
	private void pulselib$replayProvider(Model<?> model, Object state, Operation<Void> original,
			@Local(argsOnly = true) ModelFeatureRenderer.Submit<?> submit)
	{
		original.call(model, state);
		var entry = ((PArmorModelSnapshots.Node)(Object)submit).pulselib$getArmorProvider();
		if (entry != null)
			entry.snapshot().restore();
	}
}
