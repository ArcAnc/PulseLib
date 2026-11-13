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
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test-only output seam: executes native geometry preparation without Minecraft TextureManager/GPU upload. */
@Mixin(RenderTypeFeatureRenderer.class)
public abstract class FeatureVertexOutputMixin
{
	@Inject(method = "getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("HEAD"), cancellable = true)
	private void fixture$consumer(RenderType type, CallbackInfoReturnable<VertexConsumer> callback)
	{
		var consumer = FeatureChecks.currentConsumer();
		if (consumer != null)
			callback.setReturnValue(consumer);
	}
}
