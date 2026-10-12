package com.arcanc.pulselib.content.renderer;

import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.mojang.blaze3d.pipeline.RenderTarget;

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
		PRenderQueue.compositeTranslucency(destination);
	}
}
