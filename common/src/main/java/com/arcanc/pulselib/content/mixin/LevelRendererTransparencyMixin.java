/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Inserts OIT after the same vanilla transparency pass on both loaders. */
@Mixin (LevelRenderer.class)
public abstract class LevelRendererTransparencyMixin
{
	@Shadow
	private @Nullable PostChain transparencyChain;

	/**
	 * Composites PulseLib translucency together with vanilla Fabulous transparency targets.
	 *
	 * @param chain the chain
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param original the original implementation or unmodified value supplied by the render hook
	 */
	@WrapOperation (method = "renderLevel", at = @At (value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
	private void pulselib$compositeFabulous(PostChain chain, float partialTick, Operation<Void> original)
	{
		original.call(chain, partialTick);
		if (chain == this.transparencyChain)
		{
			LevelRenderer renderer = (LevelRenderer)(Object)this;
			PRenderQueue.compositeFabulousTranslucency(Minecraft.getInstance().getMainRenderTarget(), List.of(
					renderer.getTranslucentTarget(), renderer.getItemEntityTarget(), renderer.getParticlesTarget(),
					renderer.getWeatherTarget(), renderer.getCloudsTarget()));
		}
	}
}
