/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client.mixin;

import com.arcanc.pulselib.test.stage61client.TransparencyProbe;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Reads the real native/OIT framegraph pass without flushing or replacing production composition. */
@Mixin(PRenderQueue.class)
public abstract class TransparencyObserverMixin
{
	/** Negative control is isolated to this production call site, after all early/world guards. */
	@WrapOperation(method = "compositeFabulousTranslucency", at = @At(value = "INVOKE", target =
			"Lcom/arcanc/pulselib/content/renderer/gl/PGlMultiDrawExecutor;compositeFabulousOit(Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/List;)V"))
	private static void resolve(PGlMultiDrawExecutor executor, RenderTarget destination,
	                            List<RenderTarget> targets, Operation<Void> original)
	{
		if (!Boolean.getBoolean("pulselib.fabricTransparencyNegative"))
			original.call(executor, destination, targets);
	}

	@Inject(method = "compositeFabulousTranslucency", at = @At("HEAD"))
	private static void before(RenderTarget destination, List<RenderTarget> targets, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.fabricTransparencyProbe"))
			TransparencyProbe.composite(destination, targets, false);
	}

	@Inject(method = "compositeFabulousTranslucency", at = @At("RETURN"))
	private static void after(RenderTarget destination, List<RenderTarget> targets, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.fabricTransparencyProbe"))
			TransparencyProbe.composite(destination, targets, true);
	}
}
