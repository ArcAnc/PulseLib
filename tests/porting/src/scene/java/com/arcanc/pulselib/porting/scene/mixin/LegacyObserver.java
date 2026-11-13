/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.porting.scene.SceneChecks;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A legacy fallback must not be mistaken for an RHI scene passing. Does not cancel the fallback. */
@Mixin(value = PGlMultiDrawExecutor.class, remap = false)
public abstract class LegacyObserver
{
	@Inject(method = "execute(Lcom/arcanc/pulselib/content/renderer/plan/PRenderPlan;Lcom/arcanc/pulselib/content/renderer/plan/PRenderFrame;Lcom/mojang/blaze3d/pipeline/RenderTarget;)V", at = @At("HEAD"))
	private void scene$count(PRenderPlan plan, PRenderFrame frame, RenderTarget target, CallbackInfo callback)
	{
		if (!plan.isEmpty())
			SceneChecks.recordBackend(false);
	}
}
