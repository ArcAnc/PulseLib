/**
 * @author ArcAnc
 * Created at: 24.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.content.renderer.rhi.PRhiFrameResources;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Declares the resolve inside the framegraph while all five Fabulous targets are still alive. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin
{
	@Shadow
	@Final
	private LevelTargetBundle targets;

	@Shadow
	@Final
	private SubmitNodeStorage submitNodeStorage;

	@Shadow
	@Final
	private static Identifier TRANSPARENCY_POST_CHAIN_ID;

	@Unique
	private FeatureRenderDispatcher.@Nullable PreparedFrame pulselib$preparedFrame;

	@Unique
	private boolean pulselib$modelViewPushed;

	@Unique
	private boolean pulselib$needsTransparencyTargets;

	@Unique
	private boolean pulselib$panorama;

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;pushMatrix()Lorg/joml/Matrix4fStack;"))
	private Matrix4fStack pulselib$captureWorldMatrix(Matrix4fStack stack, Operation<Matrix4fStack> original)
	{
		var result = original.call(stack);
		this.pulselib$modelViewPushed = true;
		return result;
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"))
	private Matrix4fStack pulselib$releaseWorldMatrix(Matrix4fStack stack, Operation<Matrix4fStack> original)
	{
		try
		{
			return original.call(stack);
		}
		finally
		{
			this.pulselib$modelViewPushed = false;
		}
	}

	/** Native render closes its prepared frame only on normal return; retain ownership on failure. */
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"))
	private FeatureRenderDispatcher.PreparedFrame pulselib$capturePreparedFrame(FeatureRenderDispatcher dispatcher, SubmitNodeStorage submits,
			Operation<FeatureRenderDispatcher.PreparedFrame> original)
	{
		this.pulselib$preparedFrame = original.call(dispatcher, submits);
		return this.pulselib$preparedFrame;
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;close()V"))
	private void pulselib$releasePreparedFrame(FeatureRenderDispatcher.PreparedFrame frame, Operation<Void> original)
	{
		try
		{
			original.call(frame);
		}
		finally
		{
			this.pulselib$preparedFrame = null;
		}
	}

	/** OIT/feature/framegraph failures must not retain either a native frame or queued world data. */
	@WrapMethod(method = "render")
	private void pulselib$finishWorldFrame(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, CameraRenderState camera,
			Matrix4fc modelView, GpuBufferSlice fog, Vector4f fogColor, boolean sky, Operation<Void> original)
	{
		try (var resources = new PRhiFrameResources(allocator))
		{
			try
			{
				original.call(resources, delta, outline, camera, modelView, fog, fogColor, sky);
			}
			catch (RuntimeException | Error failure)
			{
				this.submitNodeStorage.getSubmitsPerOrder().clear();
				throw failure;
			}
			finally
			{
				this.pulselib$needsTransparencyTargets = false;
				this.pulselib$panorama = false;
				try
				{
					try
					{
						this.targets.clear();
						if (this.pulselib$modelViewPushed)
						{
							this.pulselib$modelViewPushed = false;
							RenderSystem.getModelViewStack().popMatrix();
						}
					}
					finally
					{
						var frame = this.pulselib$preparedFrame;
						this.pulselib$preparedFrame = null;
						if (frame != null)
							frame.close();
					}
				}
				finally
				{
					PRenderQueue.endWorldFrame();
				}
			}
		}
	}

	/** Minecraft consumes the owner lists before it chooses the native transparency chain. */
	@Inject(method = "submitFeatures", at = @At("HEAD"))
	private void pulselib$captureTransparencyEligibility(LevelRenderState state, SubmitNodeCollector collector, boolean outline, CallbackInfo callback)
	{
		this.pulselib$panorama = state.cameraRenderState.isPanoramicMode;
		this.pulselib$needsTransparencyTargets = PWorldRenderPass.requiresSeparateTargets(state);
	}

	/** Reuse the host's pooled targets and postchain without changing the extracted graphics option. */
	@Inject(method = "getTransparencyChain", at = @At("RETURN"), cancellable = true)
	private void pulselib$retainNativeTransparency(CallbackInfoReturnable<@Nullable PostChain> callback)
	{
		if (callback.getReturnValue() != null || this.pulselib$panorama)
			return;
		if (!this.pulselib$needsTransparencyTargets && !PRenderQueue.needsWorldTransparencyTargets())
			return;
		callback.setReturnValue(PLibRenderHelper.mc().getShaderManager().getPostChain(
				TRANSPARENCY_POST_CHAIN_ID, LevelTargetBundle.SORTING_TARGETS));
	}

	@Inject(method = "addAlwaysOnTopPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
	private void pulselib$addTransparencyPass(FrameGraphBuilder frame, FeatureRenderDispatcher.PreparedFrame features,
	                                          GpuBufferSlice fog, CallbackInfo callback)
	{
		PWorldRenderPass.addToFrame(frame, this.targets);
	}
}
