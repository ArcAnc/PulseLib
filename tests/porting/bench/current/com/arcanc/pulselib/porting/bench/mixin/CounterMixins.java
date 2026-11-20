/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.bench.mixin;

import com.arcanc.pulselib.porting.bench.RendererBenchmark;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

final class CounterMixins
{
	@Mixin(RenderPass.class)
	static abstract class Pass
	{
		@Inject(method = "drawIndexed(IIIII)V", at = @At("RETURN"))
		private void benchmark$indexed(int indices, int instances, int firstIndex, int baseVertex, int firstInstance, CallbackInfo callback)
		{
			RendererBenchmark.draw(indices > 0 && instances > 0 ? 1 : 0);
		}

		@Inject(method = "draw(IIII)V", at = @At("RETURN"))
		private void benchmark$array(int vertices, int instances, int firstVertex, int firstInstance, CallbackInfo callback)
		{
			RendererBenchmark.draw(vertices > 0 && instances > 0 ? 1 : 0);
		}
	}

	@Mixin(targets = "com.arcanc.pulselib.content.renderer.rhi.PRhiUploadRing", remap = false)
	static abstract class Uploads
	{
		@Inject(method = "upload", at = @At("HEAD"))
		private void benchmark$upload(CommandEncoder encoder, ByteBuffer data, CallbackInfoReturnable<GpuBuffer> callback)
		{
			RendererBenchmark.upload(data.remaining());
		}
	}

	@Mixin(RenderPass.class)
	static abstract class Indirect
	{
		@Inject(method = "drawIndexedIndirect", at = @At("RETURN"))
		private void benchmark$indirect(GpuBufferSlice commands, int count, CallbackInfo callback)
		{
			RendererBenchmark.draw(count);
		}
	}
}
