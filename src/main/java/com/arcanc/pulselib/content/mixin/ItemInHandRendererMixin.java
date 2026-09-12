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
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin
{
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void pulselib$renderFirstPersonViewmodel(
			float partialTick,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			LocalPlayer player,
			int packedLight,
			CallbackInfo ci)
	{
		PPlayerFirstPersonPose pose = PPlayerAnimations.firstPersonPose(player, partialTick);
		if (pose == null || pose.usesVanillaRenderPass())
			return;

		PPlayerFirstPersonRenderer.render(player, pose, poseStack, submitNodeCollector, packedLight, partialTick);
		ci.cancel();
	}
}
