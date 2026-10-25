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
import com.arcanc.pulselib.test.stage1.Stage43b2HelperProbe;
import net.minecraft.client.gui.render.GuiRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the complete native GUI render, without substituting its state extraction or GPU drawing. */
@Mixin(GuiRenderer.class)
public abstract class Stage43bGuiMixin
{
	@Inject(method = "render", at = @At("HEAD"))
	private void pulselib$beforeGui(GpuBufferSlice fog, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage43b2Probe"))
			Stage43b2HelperProbe.beforeGui();
		if (Boolean.getBoolean("pulselib.stage43bProbe"))
			Stage43bGuiProbe.beforeGui();
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void pulselib$afterGui(GpuBufferSlice fog, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage43b2Probe"))
			Stage43b2HelperProbe.afterGui();
		if (Boolean.getBoolean("pulselib.stage43bProbe"))
			Stage43bGuiProbe.afterGui();
	}
}
