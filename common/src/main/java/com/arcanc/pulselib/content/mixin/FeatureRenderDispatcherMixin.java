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
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

/** A failed deferred batch must be cleared just as a successful renderAllFeatures batch is. */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin
{
	@WrapMethod(method = "renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V")
	private void pulselib$discardFailedFeatures(SubmitNodeStorage storage, Operation<Void> original)
	{
		try
		{
			original.call(storage);
		}
		catch (RuntimeException | Error failure)
		{
			storage.getSubmitsPerOrder().clear();
			throw failure;
		}
	}
}
