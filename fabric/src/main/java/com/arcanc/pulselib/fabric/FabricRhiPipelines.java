/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.content.renderer.rhi.PRhiPipelines;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.ApiStatus;

/** Uses the existing production access widener during Fabric client initialization. */
@ApiStatus.Internal
public final class FabricRhiPipelines
{
	private FabricRhiPipelines()
	{
	}

	public static void register()
	{
		PRhiPipelines.portable().register(RenderPipelines::register);
	}
}
