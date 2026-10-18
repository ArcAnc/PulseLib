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

/**
 * Shared world-stage callbacks invoked by Fabric hooks and NeoForge render events.
 */
public final class PRenderStagesHandler
{
	/**
	 * Flushes geometry that must populate world depth before translucent terrain.
	 */
	public static void afterBlockEntities()
	{
		PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
		PRenderQueue.flushOpaque(PRenderQueue.RenderStage.SOLID_BLOCKS);
	}

	/**
	 * Flushes the remaining world model submissions after particle rendering.
	 */
	public static void afterParticles()
	{
		PRenderQueue.flushCombined(PRenderQueue.RenderStage.ENTITIES, PRenderQueue.RenderStage.SOLID_BLOCKS, PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS);
		PGpuDeformerBuffers.finishFrame();
	}

	/**
	 * Composites deferred translucency into the destination after weather rendering.
	 *
	 * @param destination the mutable object receiving the result
	 */
	public static void afterWeather(RenderTarget destination)
	{
		if (destination == Minecraft.getInstance().levelRenderer.getWeatherTarget())
			return;
		PRenderQueue.compositeTranslucency(destination);
	}
}
