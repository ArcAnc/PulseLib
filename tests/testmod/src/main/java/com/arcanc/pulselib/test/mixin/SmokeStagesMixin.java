package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Validates actual loader hook order without logging each render frame. */
@Mixin(PRenderStagesHandler.class)
public abstract class SmokeStagesMixin
{
	private static int phase;
	

	@Inject(method = "afterBlockEntities", at = @At("HEAD"))
	private static void pulselibtest$blocks(CallbackInfo callback)
	{
		if (phase != 0) throw new AssertionError("Block-entity stage out of order: " + phase);
		phase = 1;
	}

	@Inject(method = "afterParticles", at = @At("HEAD"))
	private static void pulselibtest$particles(CallbackInfo callback)
	{
		if (phase != 1) throw new AssertionError("Particle stage out of order: " + phase);
		phase = 2;
	}

	@Inject(method = "afterWeather", at = @At("HEAD"))
	private static void pulselibtest$weather(CallbackInfo callback)
	{
		if (phase != 2) throw new AssertionError("Weather stage out of order: " + phase);
		phase = 0;
		com.arcanc.pulselib.test.ClientTestPlugin.stageFrames++;
	}
}
