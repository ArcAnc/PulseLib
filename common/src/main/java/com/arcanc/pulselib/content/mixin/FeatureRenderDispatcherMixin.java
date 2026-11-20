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
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Releases a deferred batch even if preparing it fails before the caller receives a frame. */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin
{
	@Unique
	private FeatureRenderDispatcher.@Nullable PreparedFrame pulselib$preparing;

	@WrapOperation(method = "prepareFrameWithContext", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;begin(Lnet/minecraft/client/renderer/feature/FeatureFrameContext;Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"))
	private FeatureRenderDispatcher.PreparedFrame pulselib$beginFrame(FeatureRenderDispatcher.PreparedFrame frame,
			FeatureFrameContext context, SubmitNodeStorage storage, Operation<FeatureRenderDispatcher.PreparedFrame> original)
	{
		this.pulselib$preparing = original.call(frame, context, storage);
		return this.pulselib$preparing;
	}

	@WrapMethod(method = "prepareFrameWithContext")
	private FeatureRenderDispatcher.PreparedFrame pulselib$releaseFailedPreparation(FeatureFrameContext context,
			SubmitNodeStorage storage, Operation<FeatureRenderDispatcher.PreparedFrame> original)
	{
		var previous = this.pulselib$preparing;
		this.pulselib$preparing = null;
		try
		{
			return original.call(context, storage);
		}
		catch (RuntimeException | Error failure)
		{
			try (var frame = this.pulselib$preparing)
			{
				storage.getSubmitsPerOrder().clear();
				throw failure;
			}
		}
		finally
		{
			this.pulselib$preparing = previous;
		}
	}

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
