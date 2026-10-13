/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.H14Harness;
import com.arcanc.pulselib.test.DisposableTestWorlds;
import com.arcanc.pulselib.test.OitPerformanceHarness;
import com.arcanc.pulselib.test.WorldOitPerformanceHarness;
import com.arcanc.pulselib.test.SmokeHarness;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (GameRenderer.class)
public abstract class SmokeRenderMixin
{
	@Inject (method = "render", at = @At ("HEAD"))
	private void pulselibtest$fixedCamera(CallbackInfo callback)
	{
		DisposableTestWorlds.prepareAutomationWindow();
		H14Harness.lockWorldCamera();
		SmokeHarness.lockWorldCamera();
		if (Boolean.getBoolean("pulselib.test.oitPerf") && Boolean.getBoolean("pulselib.test.world"))
		{
			WorldOitPerformanceHarness.beginFrame();
		}
	}

	@Inject (method = "render", at = @At ("TAIL"))
	private void pulselibtest$checkResources(CallbackInfo callback)
	{
		DisposableTestWorlds.loadIfRequested();
		WorldOitPerformanceHarness.endFrame();
		if (Boolean.getBoolean("pulselib.test.oitPerf"))
		{
			OitPerformanceHarness.frame();
		}
		else if (Boolean.getBoolean("pulselib.test.oit"))
		{
			H14Harness.frame();
		}
		else if (Boolean.getBoolean("pulselib.test.auto"))
		{
			SmokeHarness.frame();
		}
	}
}
