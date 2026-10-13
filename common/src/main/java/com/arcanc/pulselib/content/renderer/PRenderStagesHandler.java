/**
 * @author ArcAnc
 * Created at: 15.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

public final class PRenderStagesHandler
{
	public static void afterBlockEntities()
	{
		PRenderQueue.flush(PRenderQueue.RenderStage.SOLID_BLOCKS);
	}

	public static void afterParticles()
	{
		PRenderQueue.flushCombined(PRenderQueue.RenderStage.ENTITIES, PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS);
		PGpuDeformerBuffers.finishFrame();
	}

	public static void afterWeather(RenderTarget destination)
	{
		// Fabulous still has to combine rain, clouds, particles and terrain.
		// Keep the OIT layers until that composition has completed.
		if (destination == Minecraft.getInstance().levelRenderer.getWeatherTarget())
		{
			return;
		}
		PRenderQueue.compositeTranslucency(destination);
	}
}
