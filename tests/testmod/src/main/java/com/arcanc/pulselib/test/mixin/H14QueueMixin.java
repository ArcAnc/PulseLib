/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.test.H14Harness;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin (PRenderQueue.class)
public abstract class H14QueueMixin
{
	@Inject (method = "compositeFabulousTranslucency", at = @At ("HEAD"))
	private static void pulselibtest$composite(RenderTarget destination, List<RenderTarget> targets, CallbackInfo callback)
	{
		H14Harness.fabulousWorldComposite(destination, targets);
	}
}
