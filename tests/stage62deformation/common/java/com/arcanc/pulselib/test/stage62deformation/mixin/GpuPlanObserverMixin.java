/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation.mixin;

import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.test.stage62deformation.InstanceParityProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads the actual compiled plan entering the existing executor, without consuming or replacing it. */
@Mixin(PGlMultiDrawExecutor.class)
public abstract class GpuPlanObserverMixin
{
	@Inject(method = "executeImmediate", at = @At("HEAD"))
	private void plan(PRenderPlan plan, PRenderFrame frame, CallbackInfo callback)
	{
		InstanceParityProbe.observe(plan);
	}
}
