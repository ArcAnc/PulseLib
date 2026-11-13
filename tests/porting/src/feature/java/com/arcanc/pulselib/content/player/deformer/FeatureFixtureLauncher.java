/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.startup.Entrypoint;

/** Real native CLIENT/Mixin environment, without creating Minecraft or a world. */
public final class FeatureFixtureLauncher extends Entrypoint
{
	public static void main(String[] args) throws Throwable
	{
		try (var startup = startup(new String[0], true, Dist.CLIENT, true))
		{
			var main = createMainMethodCallable(startup, "com.arcanc.pulselib.content.player.deformer.FeatureChecks");
			main.invokeExact(args);
		}
	}
}
