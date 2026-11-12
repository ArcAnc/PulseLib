/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.PPlayerModelSubmitState;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
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
import org.spongepowered.asm.mixin.injection.At;

/** Restores the shared player model after all native main/outline/crumbling deferred draws. */
@Mixin(ModelFeatureRenderer.class)
public abstract class PlayerModelFeatureMixin
{
	@Shadow
	@Final
	private PoseStack poseStack;

	@WrapMethod(method = "renderModel")
	private void pulselib$restoreDeferredPlayer(SubmitNodeStorage.ModelSubmit<?> submit,
	                                            RenderType type,
	                                            VertexConsumer buffer,
	                                            OutlineBufferSource outlines,
	                                            MultiBufferSource.BufferSource crumbling,
	                                            Operation<Void> original)
	{
		var geometry = (Object)submit instanceof PPlayerModelSubmitState access ? access.pulselib$getModelGeometry() : null;
		if (!(submit.state() instanceof AvatarRenderState) || !(submit.model() instanceof PlayerModel) && geometry == null)
		{
			original.call(submit, type, buffer, outlines, crumbling);
			return;
		}
		int depth = ((PoseStackAccessor)this.poseStack).pulselib$getDepth();
		PoseStack.Pose entry = this.poseStack.last().copy();
		try (var snapshot = PModelPartSnapshot.capture(submit.model().root()))
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

	@WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"))
	private void pulselib$applyNodeGeometry(Model<?> model,
	                                        Object state,
	                                        Operation<Void> original,
	                                        @Local (argsOnly = true, name = "submit") SubmitNodeStorage.ModelSubmit<?> submit)
	{
		original.call(model, state);
		var entry = (Object)submit instanceof PPlayerModelSubmitState access ? access.pulselib$getModelGeometry() : null;
		if (entry == null)
			return;
		if (model instanceof HumanoidModel<?> humanoid && !(model instanceof PlayerModel) && state instanceof PPlayerRigRenderState rig && rig.pulselib$getPlayerRig() != null)
			rig.pulselib$getPlayerRig().apply(humanoid);
		entry.geometry().restore();
	}
}
