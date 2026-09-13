/**
 * @author ArcAnc
 * Created at: 29.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies PulseLib integration to {@code GameRenderer}.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
	/**
	 * Performs the pulselib$flush first person items operation.
	 * @param cameraState the camera state to use.
	 * @param deltaPartialTick the delta partial tick to use.
	 * @param modelViewMatrix the model view matrix to use.
	 * @param ci the ci to use.
	 */
	@Inject(method = "renderItemInHand", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
			shift = At.Shift.AFTER))
	private void pulselib$flushFirstPersonItems(CameraRenderState cameraState,
	                                            float deltaPartialTick,
	                                            Matrix4fc modelViewMatrix,
	                                            CallbackInfo ci)
	{
		PRenderQueue.flush(PRenderQueue.RenderStage.FIRST_PERSON);
		PRenderQueue.compositeTranslucency();
	}
}
