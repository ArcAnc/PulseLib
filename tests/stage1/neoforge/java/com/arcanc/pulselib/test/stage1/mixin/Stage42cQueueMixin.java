/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage42cLifecycleProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the real shared cleanup; it never cancels or replaces it. */
@Mixin(com.arcanc.pulselib.content.renderer.PRenderQueue.class)
public abstract class Stage42cQueueMixin
{
	@Inject(method = "cleanUp", at = @At("HEAD"))
	private static void pulselib$captureResources(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage42cProbe"))
			Stage42cLifecycleProbe.beforeCleanup();
	}

	@Inject(method = "cleanUp", at = @At("RETURN"))
	private static void pulselib$checkResources(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage42cProbe"))
			Stage42cLifecycleProbe.afterCleanup();
	}
}
