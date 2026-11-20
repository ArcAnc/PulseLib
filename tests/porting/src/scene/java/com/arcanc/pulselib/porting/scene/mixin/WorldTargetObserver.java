/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.porting.scene.WorldIntegrationChecks;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PWorldRenderPass.class, remap = false)
public abstract class WorldTargetObserver
{
	@Inject(method = "addToFrame", at = @At("HEAD"))
	private static void scene$targets(FrameGraphBuilder frame, LevelTargetBundle targets, CallbackInfo callback)
	{
		WorldIntegrationChecks.targets(targets);
	}
}
