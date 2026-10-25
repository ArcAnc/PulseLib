/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** A failed deferred batch must be cleared just as a successful renderAllFeatures batch is. */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin
{
	@Shadow
	public abstract void clearSubmitNodes();

	@WrapMethod(method = "renderAllFeatures")
	private void pulselib$discardFailedFeatures(Operation<Void> original)
	{
		try
		{
			original.call();
		}
		catch (RuntimeException | Error failure)
		{
			clearSubmitNodes();
			throw failure;
		}
	}
}
