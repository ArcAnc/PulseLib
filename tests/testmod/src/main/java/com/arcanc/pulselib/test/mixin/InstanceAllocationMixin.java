/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.test.GpuTiming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts fallback instance/indirect storage replacement, if selected by the host. */
@Mixin (targets = "com.arcanc.pulselib.content.renderer.legacy.GlDrawExecutor$InstanceBuffer")
public abstract class InstanceAllocationMixin
{
	@Inject (method = "upload", at = @At (value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glBufferData(ILjava/nio/ByteBuffer;I)V"))
	private void pulselibtest$bufferData(CallbackInfo callback)
	{
		GpuTiming.bufferReallocation();
	}
}
