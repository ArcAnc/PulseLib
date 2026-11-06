/**
 * @author ArcAnc
 * Created at: 06.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.test.RegressionChecks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes real maintenance; opt-in negative control suppresses native removal only. */
@Mixin(value = InstanceAnimationManager.class, remap = false)
public abstract class AnimationMaintenanceMixin
{
	@Inject(method = "removeUnused(J)V", at = @At("TAIL"), require = 1)
	private static void maintained(long now, CallbackInfo ci)
	{
		if (Boolean.getBoolean("pulselib.test.animationRegression"))
			RegressionChecks.maintained();
	}

	@Inject(method = "removeManager(Ljava/lang/Object;)V", at = @At("HEAD"), cancellable = true, require = 1)
	private static void removing(Object owner, CallbackInfo ci)
	{
		if (Boolean.getBoolean("pulselib.test.animationRegression") && Boolean.getBoolean("pulselib.test.skipAnimationRemoval"))
			ci.cancel();
	}
}
