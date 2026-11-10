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
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.MappableRingBuffer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Counts the actual physical GPU buffers created for a replacement deformer ring. */
@Mixin(targets = "com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers$UploadBuffers")
public abstract class StreamAllocationMixin
{
	@ModifyExpressionValue(method = "upload(Lnet/minecraft/client/renderer/MappableRingBuffer;Ljava/util/List;ZLjava/lang/String;)Lnet/minecraft/client/renderer/MappableRingBuffer;", at = @At(value = "NEW", target = "(Ljava/util/function/Supplier;II)Lnet/minecraft/client/renderer/MappableRingBuffer;"))
	private static MappableRingBuffer pulselibtest$storage(MappableRingBuffer ring)
	{
		for (GpuBuffer buffer : (GpuBuffer[])com.arcanc.pulselib.test.PerfResources.field(ring, "buffers"))
			GpuTiming.bufferReallocation();
		return ring;
	}
}
