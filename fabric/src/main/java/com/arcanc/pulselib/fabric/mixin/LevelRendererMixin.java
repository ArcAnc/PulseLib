/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric.mixin;

import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla 1.21.1 hooks at the exact points used by NeoForge's stage dispatch.
 */
@Mixin (LevelRenderer.class)
public abstract class LevelRendererMixin
{
	@Shadow
	private @Nullable RenderTarget weatherTarget;

	// After block-entity batches and outline processing, immediately before destruction rendering.
	@Inject (method = "renderLevel", at = @At (value = "INVOKE",
		target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;long2ObjectEntrySet()Lit/unimi/dsi/fastutil/objects/ObjectSet;"))
	private void pulselib$afterBlockEntities(CallbackInfo callback)
	{
		PRenderStagesHandler.afterBlockEntities();
	}

	@Inject (method = "renderLevel", at = @At (value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", shift = At.Shift.AFTER))
	private void pulselib$afterParticles(CallbackInfo callback)
	{
		PRenderStagesHandler.afterParticles();
	}

	@Inject (method = "renderLevel", at = @At (value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", shift = At.Shift.AFTER))
	private void pulselib$afterWeather(CallbackInfo callback)
	{
		PRenderStagesHandler.afterWeather(this.weatherTarget == null ? Minecraft.getInstance().
				getMainRenderTarget() : this.weatherTarget);
	}
}
