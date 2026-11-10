/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.arcanc.pulselib.test.H14WorldHarness;
import com.arcanc.pulselib.test.WorldRegressionRun;
import com.arcanc.pulselib.test.WorldRenderRegressionHarness;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PRenderStagesHandler.class)
public abstract class WorldStagesMixin
{
	@Inject(method = "afterBlockEntities", at = @At("HEAD"))
	private static void pulselibtest$submit(CallbackInfo callback)
	{
		if (WorldRegressionRun.h13Active())
			WorldRenderRegressionHarness.submit();
		if (WorldRegressionRun.h14Active())
			H14WorldHarness.queueWorldSurface();
	}

	@Inject(method = "afterBlockEntities", at = @At("TAIL"))
	private static void pulselibtest$depth(CallbackInfo callback)
	{
		if (WorldRegressionRun.h13Active())
			WorldRenderRegressionHarness.earlyDepth();
	}

	@Inject(method = "afterWeather", at = @At("HEAD"))
	private static void pulselibtest$fancy(RenderTarget target, CallbackInfo callback)
	{
		if (WorldRegressionRun.h14Active())
			H14WorldHarness.fancyWorldComposite(target);
	}
}
