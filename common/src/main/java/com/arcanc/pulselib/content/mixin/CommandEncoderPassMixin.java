/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderPassTracker;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Backend-neutral pass lifetime used by scopes; observes native validation and never substitutes a pass. */
@Mixin(CommandEncoder.class)
public abstract class CommandEncoderPassMixin
{
	@Inject(method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At("RETURN"))
	private void pulselib$opened(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> callback)
	{
		PRhiRenderPassTracker.opened((CommandEncoder)(Object)this);
	}

	@WrapMethod(method = "submitRenderPass()V")
	private void pulselib$closed(Operation<Void> original)
	{
		try
		{
			original.call();
		}
		finally
		{
			PRhiRenderPassTracker.closed((CommandEncoder)(Object)this);
		}
	}
}
