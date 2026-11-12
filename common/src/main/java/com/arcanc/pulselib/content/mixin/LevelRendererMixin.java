/**
 * @author ArcAnc
 * Created at: 24.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PWorldRenderPass;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
	private LevelRenderState levelRenderState;

	@Shadow
	@Final
	private static Identifier TRANSPARENCY_POST_CHAIN_ID;

	/** Reuse the host's pooled targets and postchain without changing the extracted graphics option. */
	@Inject(method = "getTransparencyChain", at = @At("RETURN"), cancellable = true)
	private void pulselib$retainNativeTransparency(CallbackInfoReturnable<@Nullable PostChain> callback)
	{
		if (callback.getReturnValue() != null || !PWorldRenderPass.requiresSeparateTargets(this.levelRenderState))
			return;
		callback.setReturnValue(PLibRenderHelper.mc().getShaderManager().getPostChain(
				TRANSPARENCY_POST_CHAIN_ID, LevelTargetBundle.SORTING_TARGETS));
	}

	@Inject(method = "addLateDebugPass", at = @At("HEAD"))
	private void pulselib$addTransparencyPass(FrameGraphBuilder frame, CameraRenderState camera, GpuBufferSlice fog,
	                                          Matrix4fc modelView, CallbackInfo callback)
	{
		PWorldRenderPass.addToFrame(frame, this.targets);
	}
}
