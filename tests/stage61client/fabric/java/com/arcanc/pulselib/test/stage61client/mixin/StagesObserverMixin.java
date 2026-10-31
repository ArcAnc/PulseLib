/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client.mixin;

import com.arcanc.pulselib.test.stage61client.WorldStageProbe;

import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes actual shared stage returns; never flushes or changes production queue state. */
@Mixin(PRenderStagesHandler.class)
public abstract class StagesObserverMixin
{
	@Inject(method = "afterOpaqueFeatures", at = @At("RETURN"))
	private static void opaque(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.fabricStagesProbe"))
			WorldStageProbe.observed(0);
	}

	@Inject(method = "afterNativeFeatures", at = @At("RETURN"))
	private static void nativeDepth(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.fabricStagesProbe"))
			WorldStageProbe.observed(1);
	}

	@Inject(method = "afterParticles", at = @At("RETURN"))
	private static void particles(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.fabricStagesProbe"))
			WorldStageProbe.observed(2);
	}
}
