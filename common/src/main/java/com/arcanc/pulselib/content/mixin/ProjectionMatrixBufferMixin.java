/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderHost;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retains the CPU projection corresponding to each live Minecraft projection buffer. */
@Mixin (ProjectionMatrixBuffer.class)
public abstract class ProjectionMatrixBufferMixin
{
	@Shadow
	@Final
	private GpuBufferSlice bufferSlice;

	@Inject (method = "writeBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;", at = @At ("RETURN"))
	private void pulselib$captureProjection(Matrix4f projection, CallbackInfoReturnable<GpuBufferSlice> callback)
	{
		PRhiRenderHost.recordProjection(callback.getReturnValue(), projection);
	}

	@Inject (method = "close()V", at = @At ("RETURN"))
	private void pulselib$forgetProjection(CallbackInfo callback)
	{
		PRhiRenderHost.forgetProjection(this.bufferSlice);
	}
}
