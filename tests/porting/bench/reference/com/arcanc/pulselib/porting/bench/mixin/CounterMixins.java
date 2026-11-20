/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.bench.mixin;

import com.arcanc.pulselib.content.renderer.gl.PGlIndirectStream;
import com.arcanc.pulselib.content.renderer.gl.PGlInstanceStream;
import com.arcanc.pulselib.content.renderer.gl.PGlMultiDrawExecutor;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.porting.bench.RendererBenchmark;
import com.mojang.blaze3d.systems.RenderPass;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

final class CounterMixins
{
	@Mixin(RenderPass.class)
	static abstract class Pass
	{
		@Inject(method = "drawIndexed(IIII)V", at = @At("RETURN"))
		private void benchmark$indexed(int baseVertex, int firstIndex, int indices, int instances, CallbackInfo callback)
		{
			RendererBenchmark.draw(indices > 0 && instances > 0 ? 1 : 0);
		}

		@Inject(method = "draw(II)V", at = @At("RETURN"))
		private void benchmark$array(int firstVertex, int vertices, CallbackInfo callback)
		{
			RendererBenchmark.draw(vertices > 0 ? 1 : 0);
		}
	}

	@Mixin(value = PGlInstanceStream.class, remap = false)
	static abstract class Uploads
	{
		@Inject(method = "uploadHeaders", at = @At("RETURN"))
		private void benchmark$instances(List<PInstanceHeader> instances, int slot, boolean persistent, CallbackInfoReturnable<PGlInstanceStream.Upload> callback)
		{
			RendererBenchmark.upload(Math.max(PGlInstanceStream.STRIDE, instances.size() * PGlInstanceStream.STRIDE));
		}
	}

	@Mixin(value = PGlMultiDrawExecutor.class, remap = false)
	static abstract class Indirect
	{
		@Redirect(method = "draw", at = @At(value = "INVOKE",
				target = "Lorg/lwjgl/opengl/ARBMultiDrawIndirect;glMultiDrawElementsIndirect(IIJII)V"))
		private void benchmark$indirect(int topology, int indexType, long offset, int count, int stride)
		{
			ARBMultiDrawIndirect.glMultiDrawElementsIndirect(topology, indexType, offset, count, stride);
			RendererBenchmark.draw(count);
			RendererBenchmark.upload(count * PGlIndirectStream.STRIDE);
		}

		@Redirect(method = "draw", at = @At(value = "INVOKE",
				target = "Lorg/lwjgl/opengl/GL32;glDrawElementsInstancedBaseVertex(IIIJII)V"))
		private void benchmark$direct(int topology, int indices, int type, long offset, int instances, int baseVertex)
		{
			GL32.glDrawElementsInstancedBaseVertex(topology, indices, type, offset, instances, baseVertex);
			RendererBenchmark.draw(indices > 0 && instances > 0 ? 1 : 0);
		}
	}
}
