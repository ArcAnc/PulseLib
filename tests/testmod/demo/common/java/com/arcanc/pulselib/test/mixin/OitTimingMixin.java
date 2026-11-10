/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.GpuTiming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin (targets = "com.arcanc.pulselib.content.renderer.gl.PGlWeightedBlendedOit")
public abstract class OitTimingMixin
{
	@Unique
	private boolean pulselibtest$pass;

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

	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "composite(Lcom/mojang/blaze3d/textures/GpuTextureView;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;draw(II)V"))
	private void pulselibtest$ordinary(com.mojang.blaze3d.systems.RenderPass pass, int first, int count, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original)
	{
		draw(pass, first, count, original, "compositor");
	}

	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "compositeFabulous", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;draw(II)V", ordinal = 0))
	private void pulselibtest$metadata(com.mojang.blaze3d.systems.RenderPass pass, int first, int count, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original)
	{
		draw(pass, first, count, original, "metadata");
	}

	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "compositeFabulous", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;draw(II)V", ordinal = 1))
	private void pulselibtest$compositor(com.mojang.blaze3d.systems.RenderPass pass, int first, int count, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original)
	{
		draw(pass, first, count, original, "compositor");
	}

	@Unique
	private static void draw(com.mojang.blaze3d.systems.RenderPass pass, int first, int count, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original, String name)
	{
		GpuTiming.fullscreen();
		GpuTiming.begin(name);
		try
		{
			original.call(pass, first, count);
		}
		finally
		{
			GpuTiming.end();
		}
	}

	@Inject (method = "createTexture", at = @At ("HEAD"))
	private static void pulselibtest$textureAllocation(String label, com.mojang.blaze3d.textures.TextureFormat declaredFormat, int internalFormat, int format, int width, int height,
	                                               CallbackInfoReturnable<?> callback)
	{
		GpuTiming.allocation();
	}
}
