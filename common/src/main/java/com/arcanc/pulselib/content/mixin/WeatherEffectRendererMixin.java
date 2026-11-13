/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Weather needs its real layer depth whenever the framegraph retains a separate weather target. */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin
{
	@Redirect(method = "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/GameRenderState;useShaderTransparency()Z"))
	private boolean pulselib$writeSeparatedWeatherDepth(GameRenderState state)
	{
		return state.useShaderTransparency() || Minecraft.getInstance().levelRenderer.weatherTarget() != null;
	}
}
