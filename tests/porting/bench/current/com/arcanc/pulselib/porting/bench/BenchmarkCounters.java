/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.bench;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector4f;

final class BenchmarkCounters
{
	static final boolean REVERSED = true;

	private BenchmarkCounters()
	{
	}

	static TextureTarget target(int size)
	{
		return new TextureTarget("PulseLib performance", size, size, true, GpuFormat.RGBA8_UNORM);
	}

	static void clear(TextureTarget target)
	{
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
				target.getColorTexture(), new Vector4f(0.0f), target.getDepthTexture(), 0.0);
	}

	static void finishFrame()
	{
		RenderSystem.getDevice().createCommandEncoder().submit();
		RenderSystem.getDynamicUniforms().reset();
	}
}
