/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.content.renderer.rhi.PRhiOitRenderer;
import com.arcanc.pulselib.porting.scene.WorldChecks;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Reads native pixels around the real framegraph composite without replacing it. */
@Mixin(value = PRhiOitRenderer.class, remap = false)
public abstract class WorldCompositeObserver
{
	@Inject(method = "compositeFabulous", at = @At("HEAD"))
	private void scene$before(GpuTextureView destination, List<RenderTarget> targets, CallbackInfo callback)
	{
		if (WorldChecks.abortRequested())
			throw new WorldChecks.ExpectedWorldAbort();
		WorldChecks.beforeComposite(destination, targets);
	}

	@Inject(method = "compositeFabulous", at = @At("RETURN"))
	private void scene$after(GpuTextureView destination, List<RenderTarget> targets, CallbackInfo callback)
	{
		WorldChecks.afterComposite();
	}
	@Inject(method = "composite(Lcom/mojang/blaze3d/textures/GpuTextureView;)V", at = @At("HEAD"))
	private void scene$beforeOrdinary(GpuTextureView destination, CallbackInfo callback)
	{
		WorldChecks.beforeComposite(destination, List.of());
	}

	@Inject(method = "composite(Lcom/mojang/blaze3d/textures/GpuTextureView;)V", at = @At("RETURN"))
	private void scene$afterOrdinary(GpuTextureView destination, CallbackInfo callback)
	{
		WorldChecks.afterComposite();
	}

}
