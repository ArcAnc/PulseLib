/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.arcanc.pulselib.test.VisibilityHarness;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

/** Query boundaries close on exceptions and never consume or replace the real flush. */
@Mixin(PRenderStagesHandler.class)
public abstract class VisibilityStagesMixin
{
	@WrapMethod(method = {"afterBlockEntities", "afterParticles"})
	private static void pulselibtest$samples(Operation<Void> original)
	{
		boolean started = VisibilityHarness.beginSamples();
		try
		{
			original.call();
		}
		finally
		{
			VisibilityHarness.endSamples(started);
		}
	}
}
