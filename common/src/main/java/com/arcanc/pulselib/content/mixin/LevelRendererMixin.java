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
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Declares the resolve inside the framegraph while all five Fabulous targets are still alive. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin
{
	@Shadow
	@Final
	private LevelTargetBundle targets;

	@Inject(method = "addLateDebugPass", at = @At("HEAD"))
	private void pulselib$addTransparencyPass(FrameGraphBuilder frame, CameraRenderState camera, GpuBufferSlice fog,
	                                          Matrix4fc modelView, CallbackInfo callback)
	{
		PWorldRenderPass.addToFrame(frame, this.targets);
	}
}
