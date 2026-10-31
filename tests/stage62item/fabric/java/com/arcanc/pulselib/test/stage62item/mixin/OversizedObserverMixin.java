/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62item.mixin;

import com.arcanc.pulselib.test.stage62item.ItemClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts real native item rendering calls within an observed GUI frame. */
@Mixin(net.minecraft.client.gui.render.pip.OversizedItemRenderer.class)
public abstract class OversizedObserverMixin
{
	@Inject(method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/OversizedItemRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
	private void observe(net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState state, com.mojang.blaze3d.vertex.PoseStack poses, CallbackInfo callback)
	{
		ItemClient.nativeItem(true, state.guiItemRenderState().itemStackRenderState());
	}
}
