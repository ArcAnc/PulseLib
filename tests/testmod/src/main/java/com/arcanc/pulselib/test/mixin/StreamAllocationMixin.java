/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.mixin;

import com.arcanc.pulselib.content.renderer.legacy.GlStreamStorage;
import com.arcanc.pulselib.test.GpuTiming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Counts actual buffer storage replacement calls separately from OIT textures. */
@Mixin (GlStreamStorage.class)
public abstract class StreamAllocationMixin
{
	@Inject (method = "upload(ILjava/util/List;I)V", at = @At ("HEAD"))
	private static void pulselibtest$bufferData(int buffer, List<Float> values, int usage, CallbackInfo callback)
	{
		GpuTiming.bufferReallocation();
	}
}
