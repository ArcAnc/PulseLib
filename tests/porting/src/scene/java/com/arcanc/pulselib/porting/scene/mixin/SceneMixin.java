/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.porting.scene.SceneChecks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only driver, after native GUI draws and while Minecraft still selects its UI lightmap. */
@Mixin(GameRenderer.class)
public abstract class SceneMixin
{
	@Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/render/GuiRenderer;endFrame()V"))
	private void scene$draw(DeltaTracker delta, boolean advance, CallbackInfo callback)
	{
		if (Boolean.getBoolean("pulselib.scene.world"))
			com.arcanc.pulselib.porting.scene.WorldChecks.tick();
		else
			SceneChecks.tick();
	}
}
