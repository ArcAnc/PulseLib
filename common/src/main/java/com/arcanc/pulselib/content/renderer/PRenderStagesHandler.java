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

/** Shared 26.1 render stage operations; loader adapters own event subscription. */
public class PRenderStagesHandler
{
	public static void afterOpaqueFeatures()
	{
		PRenderQueue.flush(PRenderQueue.RenderStage.SOLID_BLOCKS);
	}

	public static void afterTranslucentFeatures()
	{
		PRenderQueue.flushCombined(PRenderQueue.RenderStage.ENTITIES, PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS);
		PRenderQueue.compositeTranslucency();
		PGpuDeformerBuffers.finishFrame();
	}
}
