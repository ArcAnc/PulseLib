/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.event.PulseRuntime;
import com.arcanc.pulselib.test.SmokeLifecycleHarness;
import com.arcanc.pulselib.test.ItemStackIdentityHarness;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (PulseRuntime.class)
public abstract class SmokeLifecycleMixin
{
	@Inject (method = "disconnected", at = @At ("HEAD"), cancellable = true)
	private static void pulselibtest$observeCleanup(CallbackInfo callback)
	{
		if (ItemStackIdentityHarness.enabled())
		{
			ItemStackIdentityHarness.observeDisconnectHook();
			if (Boolean.getBoolean("pulselib.test.identitySkipHook"))
				callback.cancel();
		}
		if (Boolean.getBoolean("pulselib.test.lifecycleRegression") && SmokeLifecycleHarness.observeDisconnectHook())
			callback.cancel();
	}

	@Inject (method = "disconnected", at = @At ("TAIL"))
	private static void pulselibtest$assertCleanup(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.test.lifecycleRegression"))
			SmokeLifecycleHarness.afterDisconnectHook();
	}
}
