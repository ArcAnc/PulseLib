/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.renderer.legacy.GlWeightedBlendedOit;
import com.arcanc.pulselib.test.GpuTiming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin (GlWeightedBlendedOit.class)
public abstract class OitTimingMixin
{
	@Unique
	private boolean pulselibtest$pass;
	@Unique
	private static boolean pulselibtest$metadata;

	@Inject (method = "beginDepthPass", at = @At ("HEAD"))
	private void pulselibtest$depth(int layer, CallbackInfo callback)
	{
		GpuTiming.begin("depth");
		this.pulselibtest$pass = true;
	}

	@Inject (method = "beginAccumulationPass", at = @At ("HEAD"))
	private void pulselibtest$accumulation(int layer, CallbackInfo callback)
	{
		GpuTiming.begin("accumulation");
		this.pulselibtest$pass = true;
	}

	@Inject (method = "endPass", at = @At ("TAIL"))
	private void pulselibtest$endPass(CallbackInfo callback)
	{
		if (this.pulselibtest$pass)
		{
			GpuTiming.end();
			this.pulselibtest$pass = false;
		}
	}

	@Inject (method = "resolveFabulousMetadata", at = @At ("HEAD"))
	private void pulselibtest$metadataStart(int layer, CallbackInfo callback)
	{
		pulselibtest$metadata = true;
		GpuTiming.begin("metadata");
	}

	@Inject (method = "resolveFabulousMetadata", at = @At ("TAIL"))
	private void pulselibtest$metadataEnd(int layer, CallbackInfo callback)
	{
		GpuTiming.end();
		pulselibtest$metadata = false;
	}

	@Inject (method = "drawScreen", at = @At ("HEAD"))
	private static void pulselibtest$screenStart(CallbackInfo callback)
	{
		GpuTiming.fullscreen();
		if (!pulselibtest$metadata)
		{
			GpuTiming.begin("compositor");
		}
	}

	@Inject (method = "drawScreen", at = @At ("TAIL"))
	private static void pulselibtest$screenEnd(CallbackInfo callback)
	{
		if (!pulselibtest$metadata)
		{
			GpuTiming.end();
		}
	}

	@Inject (method = "createTexture", at = @At ("HEAD"))
	private static void pulselibtest$textureAllocation(int internalFormat, int format, int width, int height,
	                                               CallbackInfoReturnable<Integer> callback)
	{
		GpuTiming.allocation();
	}
}
