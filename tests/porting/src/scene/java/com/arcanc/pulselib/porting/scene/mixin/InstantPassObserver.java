/**
 * @author ArcAnc
 * Created at: 14.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene.mixin;

import com.arcanc.pulselib.porting.scene.SceneChecks;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the real native GPU API; an instanced draw or instance-array binding cannot pass. */
@Mixin(RenderPass.class)
public abstract class InstantPassObserver
{
	@Unique
	private boolean scene$instant;
	@Unique
	private boolean scene$color;

	@Inject(method = "setPipeline", at = @At("HEAD"))
	private void scene$pipeline(RenderPipeline pipeline, CallbackInfo callback)
	{
		this.scene$instant = pipeline.getVertexShader().getNamespace().equals("pulselib") && pipeline.getVertexShader().getPath().equals("core/triangles_instant");
		this.scene$color = false;
	}

	@Inject(method = "setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
	private void scene$uniform(String name, GpuBufferSlice value, CallbackInfo callback)
	{
		if (!this.scene$instant)
			return;
		if (name.equals("InstanceData"))
			throw new AssertionError("Instant pass bound an instance array");
		if (name.equals("ColorOverlay"))
		{
			if (value.length() != 48)
				throw new AssertionError("Instant ColorOverlay must contain one 48-byte object");
			this.scene$color = true;
		}
	}

	@Inject(method = "drawIndexed", at = @At("HEAD"))
	private void scene$draw(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance, CallbackInfo callback)
	{
		if (!this.scene$instant || indexCount == 0)
			return;
		if (instanceCount != 1 || firstInstance != 0 || !this.scene$color)
			throw new AssertionError("Instant draw must have one object and its own ColorOverlay uniforms");
		SceneChecks.recordInstantDraw();
	}
}
