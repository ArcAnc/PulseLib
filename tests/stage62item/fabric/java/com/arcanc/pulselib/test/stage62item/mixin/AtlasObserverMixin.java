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
@Mixin(net.minecraft.client.gui.render.GuiItemAtlas.class)
public abstract class AtlasObserverMixin
{
	@Inject(method = "drawToSlot", at = @At("HEAD"))
	private void observe(int x, int y, boolean clear, net.minecraft.client.renderer.item.ItemStackRenderState state, CallbackInfo callback)
	{
		ItemClient.nativeItem(false, state);
	}
}
