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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** Native layers may set up the shared player model while constructing deferred submissions. */
@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRigSubmitMixin
{
	@WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
	private void pulselib$restorePlayerModel(LivingEntityRenderState state, PoseStack poses,
			SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original)
	{
		if (!(state instanceof AvatarRenderState))
		{
			original.call(state, poses, collector, camera);
			return;
		}
		var model = ((LivingEntityRenderer<?, ?, ?>)(Object)this).getModel();
		try (var snapshot = PModelPartSnapshot.capture(model.root()))
		{
			original.call(state, poses, collector, camera);
		}
	}
}
