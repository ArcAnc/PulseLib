package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.SmokeHarness;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SmokeRenderMixin
{
	@Inject(method = "render", at = @At("TAIL"))
	private void pulselibtest$checkResources(CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.test.auto"))
			SmokeHarness.frame();
	}
}
