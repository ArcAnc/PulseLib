/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62layers.mixin;

import com.arcanc.pulselib.test.stage62layers.LayersProbe;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Attributions only; the real native renderer still calls its registered Common layer. */
@Mixin(PLivingAttachmentLayer.class)
public abstract class LayerObserverMixin
{
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"))
	private void observe(PoseStack poses, SubmitNodeCollector collector, int light, LivingEntityRenderState state, float yRot, float xRot, CallbackInfo callback)
	{
		LayersProbe.drawing(state);
	}
}
