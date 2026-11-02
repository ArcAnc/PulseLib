/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62armor.mixin;

import com.arcanc.pulselib.test.stage62armor.ArmorProbe;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes actual native GUI dispatch before and after armor drawing. */
@Mixin(GuiRenderer.class)
public abstract class GuiObserverMixin
{
	@Inject(method = "render", at = @At("HEAD"))
	private void before(GpuBufferSlice fog, CallbackInfo callback)
	{
		ArmorProbe.beforeGui();
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void after(GpuBufferSlice fog, CallbackInfo callback)
	{
		ArmorProbe.afterGui();
	}
}
