/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.content.renderer.rhi.PRhiPipelines;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.jetbrains.annotations.ApiStatus;

/** Client mod-bus registration; the backend definitions remain owned by Common. */
@ApiStatus.Internal
public final class NeoForgeRhiPipelines
{
	private NeoForgeRhiPipelines()
	{
	}

	public static void register(RegisterRenderPipelinesEvent event)
	{
		PRhiPipelines.portable().register(event::registerPipeline);
	}
}
