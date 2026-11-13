/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.deformer.FeatureChecks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test-only foil output; native main/foil iteration and all production restoration hooks still execute. */
@Mixin(ItemFeatureRenderer.class)
public abstract class FeatureItemOutputMixin
{
	@Inject(method = "getFoilBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("HEAD"), cancellable = true)
	private void fixture$foil(RenderType type, PoseStack.@Nullable Pose pose, CallbackInfoReturnable<VertexConsumer> callback)
	{
		var consumer = FeatureChecks.currentConsumer();
		if (consumer != null)
			callback.setReturnValue(consumer);
	}
}
