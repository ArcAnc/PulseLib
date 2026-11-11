/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.DemoProbe;
import com.arcanc.pulselib.test.LegacyClipHarness;
import com.arcanc.pulselib.test.CanonicalRun;
import com.arcanc.pulselib.test.GpuFacadeProbe;
import com.arcanc.pulselib.test.MaterialApiProbe;
import com.arcanc.pulselib.test.PlayerMotionRun;
import com.arcanc.pulselib.test.GlErrors;
import com.arcanc.pulselib.test.H14OffscreenHarness;
import com.arcanc.pulselib.test.SmokeHarness;
import com.arcanc.pulselib.test.WorldRegressionRun;
import com.arcanc.pulselib.test.RenderRegressionRun;
import com.arcanc.pulselib.test.VisibilityHarness;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SmokeRenderMixin
{
	@Inject(method = "update", at = @At("HEAD"))
	private void pulselibtest$fixedCamera(CallbackInfo callback)
	{
		SmokeHarness.prepareAutomationWindow();
		DemoProbe.lockSceneCamera();
		SmokeHarness.lockWorldCamera();
		WorldRegressionRun.lockCamera();
		VisibilityHarness.lockCamera();
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void pulselibtest$graphics(CallbackInfo callback)
	{
		if (LegacyClipHarness.enabled())
			LegacyClipHarness.frame();
		PlayerMotionRun.frame();
		if (VisibilityHarness.enabled())
		{
			GlErrors.check();
			VisibilityHarness.frame();
		}
		if (RenderRegressionRun.enabled())
		{
			GlErrors.check();
			RenderRegressionRun.frame();
		}
		if (WorldRegressionRun.enabled())
		{
			GlErrors.check();
			WorldRegressionRun.frame();
		}
		if (H14OffscreenHarness.enabled())
		{
			GlErrors.check();
			H14OffscreenHarness.frame();
		}
		if (SmokeHarness.enabled())
		{
			GlErrors.check();
			SmokeHarness.frame();
		}
		if (CanonicalRun.noWorldSmoke() && SmokeHarness.completed())
		{
			GpuFacadeProbe.run();
			MaterialApiProbe.run();
		}
		CanonicalRun.frame();
	}
}
