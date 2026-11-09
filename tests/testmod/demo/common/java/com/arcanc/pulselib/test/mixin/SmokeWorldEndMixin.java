/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.test.SmokeStages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PRenderQueue.class)
public abstract class SmokeWorldEndMixin
{
	@Inject(method = "endWorldFrame", at = @At("HEAD"))
	private static void pulselibtest$endFrame(CallbackInfo callback)
	{
		SmokeStages.endFrame();
	}
}
