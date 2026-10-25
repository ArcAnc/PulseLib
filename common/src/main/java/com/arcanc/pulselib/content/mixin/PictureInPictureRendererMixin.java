/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Isolates actual PIP submission and feature execution, after native attachments/projection are selected. */
@Mixin(PictureInPictureRenderer.class)
public abstract class PictureInPictureRendererMixin
{
	@WrapMethod(method = "prepare")
	private void pulselib$restoreCaller(PictureInPictureRenderState state, GuiRenderState gui, int scale, Operation<Void> original)
	{
		try (var caller = new GlRenderState())
		{
			original.call(state, gui, scale);
		}
	}

	@WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/pip/PictureInPictureRenderer;renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void pulselib$renderLocal(PictureInPictureRenderer<?> renderer, PictureInPictureRenderState state,
	                                  PoseStack poses, Operation<Void> original)
	{
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.GUI))
		{
			try
			{
				original.call(renderer, state, poses);
			}
			catch (RuntimeException | Error failure)
			{
				scope.discard();
				throw failure;
			}
		}
	}
}
