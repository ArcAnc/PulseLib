/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.startup.Entrypoint;

/** Uses FML's real dedicated startup and content loader, without entering Minecraft server/world creation. */
public final class NeoForgeFixtureLauncher extends Entrypoint
{
	public static void main(String[] args) throws Throwable
	{
		try (var startup = startup(args, true, Dist.DEDICATED_SERVER, true))
		{
			var main = createMainMethodCallable(startup, "com.arcanc.pulselib.porting.NeoForgeBootstrapChecks");
			main.invokeExact(new String[0]);
		}
	}
}
