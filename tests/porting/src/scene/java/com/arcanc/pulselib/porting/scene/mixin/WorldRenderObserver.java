/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.porting.scene.WorldChecks;
import com.arcanc.pulselib.porting.scene.WorldIntegrationChecks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Only the fixture's explicitly marked exception is handled, after the production framegraph finally. */
@Mixin(GameRenderer.class)
public abstract class WorldRenderObserver
{
	@WrapMethod(method = "renderLevel")
	private void scene$expectedFailure(DeltaTracker delta, Operation<Void> original)
	{
		var matrix = RenderSystem.getModelViewMatrixCopy();
		WorldIntegrationChecks.beginFrame();
		try
		{
			original.call(delta);
			WorldIntegrationChecks.endFrame();
		}
		catch (WorldChecks.ExpectedWorldAbort expected)
		{
			WorldChecks.recoveredAbort(matrix);
		}
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"))
	private void scene$expectedHandFailure(GameRenderer renderer, CameraRenderState camera, float tick, Matrix4fc transform, Operation<Void> original)
	{
		var matrix = RenderSystem.getModelViewMatrixCopy();
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var color = RenderSystem.outputColorTextureOverride;
		var depth = RenderSystem.outputDepthTextureOverride;
		try
		{
			original.call(renderer, camera, tick, transform);
		}
		catch (WorldIntegrationChecks.ExpectedHandAbort expected)
		{
			if (RenderSystem.getProjectionMatrixBuffer() != projection || RenderSystem.outputColorTextureOverride != color ||
					RenderSystem.outputDepthTextureOverride != depth)
				throw new AssertionError("Hand failure leaked projection or target overrides", expected);
			WorldIntegrationChecks.recoverHand(matrix);
		}
	}
}
