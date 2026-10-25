/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage43bGuiProbe;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiItemAtlas.class)
public abstract class Stage43bNativeGuiMixin
{
	@Inject(method = "drawToSlot", at = @At("HEAD"))
	private void pulselib$observeAtlas(int x, int y, boolean clear, ItemStackRenderState state, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage43bProbe"))
			Stage43bGuiProbe.atlas(false);
	}
}
