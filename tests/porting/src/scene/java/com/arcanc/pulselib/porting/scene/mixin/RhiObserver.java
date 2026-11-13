/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.rhi.PRhiDrawExecutor;
import com.arcanc.pulselib.porting.scene.SceneChecks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes production dispatch without changing its arguments, result or execution. */
@Mixin(value = PRhiDrawExecutor.class, remap = false)
public abstract class RhiObserver
{
	@Inject(method = "execute(Lcom/arcanc/pulselib/content/renderer/plan/PRenderPlan;Lcom/arcanc/pulselib/content/renderer/plan/PRenderFrame;)V", at = @At("HEAD"))
	private void scene$count(PRenderPlan plan, PRenderFrame frame, CallbackInfo callback)
	{
		if (!plan.isEmpty())
			SceneChecks.recordBackend(true);
	}
}
