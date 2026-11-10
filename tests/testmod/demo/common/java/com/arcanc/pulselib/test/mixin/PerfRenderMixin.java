/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.PerfRun;
import com.arcanc.pulselib.test.WorldOitPerformanceHarness;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/** Timestamp the actual native render call; drain queries and write rows after the interval. */
@Mixin(GameRenderer.class)
public abstract class PerfRenderMixin
{
	@WrapMethod(method = "render")
	private void pulselibtest$profile(DeltaTracker delta, boolean tick, Operation<Void> original)
	{
		if (PerfRun.world())
			WorldOitPerformanceHarness.beginFrame();
		try
		{
			original.call(delta, tick);
		}
		finally
		{
			if (PerfRun.world())
				WorldOitPerformanceHarness.endFrame();
		}
		PerfRun.frame();
	}
}
