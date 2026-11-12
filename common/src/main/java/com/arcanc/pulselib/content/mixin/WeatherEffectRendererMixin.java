/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Weather needs its real layer depth whenever the framegraph retains a separate weather target. */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin
{
	@Redirect(method = "render*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useShaderTransparency()Z"))
	private boolean pulselib$writeSeparatedWeatherDepth()
	{
		return Minecraft.useShaderTransparency() || PLibRenderHelper.mc().levelRenderer.getWeatherTarget() != null;
	}
}
