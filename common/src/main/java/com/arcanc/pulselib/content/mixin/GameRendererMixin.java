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
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies PulseLib integration to {@code GameRenderer}.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
	@Shadow
	@Final
	private SubmitNodeStorage handAndScreenSubmitNodeStorage;

	@Unique
	private boolean pulselib$handModelViewPushed;

	@WrapMethod(method = "renderLevel")
	private void pulselib$worldInvocation(DeltaTracker delta, Operation<Void> original)
	{
		try (var scope = PRenderContexts.enter(PRenderContexts.Context.WORLD))
		{
			original.call(delta);
		}
	}

	@WrapMethod(method = "renderItemInHand")
	private void pulselib$handInvocation(CameraRenderState camera, float partialTick, Matrix4fc modelView, Operation<Void> original)
	{
		try (var caller = new PRhiRenderState(); var scope = PRenderContexts.enter(PRenderContexts.Context.FIRST_PERSON))
		{
			try
			{
				original.call(camera, partialTick, modelView);
			}
			catch (RuntimeException | Error failure)
			{
				this.handAndScreenSubmitNodeStorage.getSubmitsPerOrder().clear();
				throw failure;
			}
			finally
			{
				if (this.pulselib$handModelViewPushed)
				{
					this.pulselib$handModelViewPushed = false;
					RenderSystem.getModelViewStack().popMatrix();
				}
			}
		}
	}

	@WrapOperation(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;pushMatrix()Lorg/joml/Matrix4fStack;"))
	private Matrix4fStack pulselib$pushHandModelView(Matrix4fStack stack, Operation<Matrix4fStack> original)
	{
		var result = original.call(stack);
		this.pulselib$handModelViewPushed = true;
		return result;
	}

	@Inject(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;", shift = At.Shift.AFTER))
	private void pulselib$handModelViewPopped(CameraRenderState camera, float partialTick, Matrix4fc modelView, CallbackInfo callback)
	{
		this.pulselib$handModelViewPushed = false;
	}

	/** Deferred item features have finished submitting and writing depth at this point. */
	@Inject(method = "renderItemInHand", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V",
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
