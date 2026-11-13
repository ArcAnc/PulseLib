/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Initializes the actual CLIENT game class loader and access widener, without launching Minecraft. */
public final class FabricPipelineLauncher
{
	public static void main(String[] args) throws Exception
	{
		ClassLoader game = new Knot(EnvType.CLIENT).init(args);
		Thread.currentThread().setContextClassLoader(game);
		game.loadClass("com.arcanc.pulselib.content.renderer.rhi.FabricPipelineChecks").getMethod("main", String[].class).
				invoke(null, (Object)new String[0]);
	}
}
