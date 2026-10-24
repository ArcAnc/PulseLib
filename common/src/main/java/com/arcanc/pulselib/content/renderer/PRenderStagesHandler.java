/**
 * @author ArcAnc
 * Created at: 04.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;

/** Shared 26.1 world-stage operations; loader adapters own event subscription. */
public class PRenderStagesHandler
{
	/** Populates world depth before Minecraft copies it into translucent targets. */
	public static void afterOpaqueFeatures()
	{
		afterBlockEntities();
	}

	/** Canonical callback after world entity and block-entity submissions are collected. */
	public static void afterBlockEntities()
	{
		PRenderQueue.flushOpaque(PRenderQueue.RenderStage.ENTITIES);
		PRenderQueue.flushOpaque(PRenderQueue.RenderStage.SOLID_BLOCKS);
	}

	/**
	 * Compatibility entry point for loader integrations using the former callback name.
	 * Invoke once after translucent particles, after all world model submissions exist.
	 */
	public static void afterTranslucentFeatures()
	{
		afterParticles();
	}

	/** Sorts ordinary alpha across every world source while retaining separate OIT groups. */
	public static void afterParticles()
	{
		PRenderQueue.flushCombined(PRenderQueue.RenderStage.ENTITIES, PRenderQueue.RenderStage.SOLID_BLOCKS,
				PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS);
		PRenderQueue.compositeTranslucency();
		PGpuDeformerBuffers.finishFrame();
	}
}
