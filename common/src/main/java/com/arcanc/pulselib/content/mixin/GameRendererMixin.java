/**
 * @author ArcAnc
 * Created at: 29.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scopes first-person model submissions and composites them after held-item rendering.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
	/**
	 * Runs held-item rendering in the first-person submission context.
	 *
	 * @param camera the camera
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param view the view
	 * @param original the original implementation or unmodified value supplied by the render hook
	 */
	@WrapMethod (method = "renderItemInHand")
	private void pulselib$handContext(Camera camera, float partialTick, Matrix4f view, Operation<Void> original)
	{
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.FIRST_PERSON))
		{
			original.call(camera, partialTick, view);
		}
	}

	/**
	 * Flushes first-person geometry and composites its translucency after vanilla hand rendering.
	 *
	 * @param camera the camera
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param modelViewMatrix the model view matrix
	 * @param ci the Mixin callback for the intercepted method
	 */
	@Inject(method = "renderItemInHand", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
			shift = At.Shift.AFTER))
	private void pulselib$flushFirstPersonItems(Camera camera, float partialTick, Matrix4f modelViewMatrix, CallbackInfo ci)
	{
		PRenderQueue.flush(PRenderQueue.RenderStage.FIRST_PERSON);
		PRenderQueue.compositeTranslucency(Minecraft.getInstance().getMainRenderTarget());
	}
}
