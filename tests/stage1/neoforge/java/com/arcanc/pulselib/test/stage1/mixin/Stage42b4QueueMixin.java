/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage42b4TranslucentProbe;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Reads the native postchain result immediately before the production OIT resolve. */
@Mixin(PRenderQueue.class)
public abstract class Stage42b4QueueMixin
{
	@Inject(method = "compositeFabulousTranslucency", at = @At("HEAD"))
	private static void pulselib$checkNativeResolve(RenderTarget destination, List<RenderTarget> targets, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.stage42b4Probe"))
			Stage42b4TranslucentProbe.beforeComposite(destination);
	}
}
