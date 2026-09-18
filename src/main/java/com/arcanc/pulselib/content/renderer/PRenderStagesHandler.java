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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Handles render stages.
 */
public class PRenderStagesHandler
{
	/**
	 * Performs the register operation.
	 * @param modEventBus the mod event bus to use.
	 */
	public static void register(IEventBus modEventBus)
	{
		NeoForge.EVENT_BUS.addListener(PRenderStagesHandler :: renderSolid);
		
		NeoForge.EVENT_BUS.addListener(PRenderStagesHandler :: renderTranslucent);
	}
	
	/**
	 * Renders the solid.
	 * @param event the event to use.
	 */
	private static void renderSolid(final RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		PRenderQueue.flush(PRenderQueue.RenderStage.SOLID_BLOCKS);
	}
	
	/**
	 * Renders the translucent.
	 * @param event the event to use.
	 */
	private static void renderTranslucent(final RenderLevelStageEvent.AfterTranslucentFeatures event)
	{
		PRenderQueue.flushCombined(PRenderQueue.RenderStage.ENTITIES, PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS);
		PRenderQueue.compositeTranslucency();
		PGpuDeformerBuffers.finishFrame();
	}
}
