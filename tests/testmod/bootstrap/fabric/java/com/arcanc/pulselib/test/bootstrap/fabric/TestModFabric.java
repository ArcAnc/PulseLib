/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap.fabric;

import com.arcanc.pulselib.test.bootstrap.BootstrapChecks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class TestModFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		BootstrapChecks.runDirectory();
		BootstrapChecks.jar(TestModFabric.class);
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
		{
			if (server.isDedicatedServer())
				BootstrapChecks.serverStarted(server);
		});
		ServerTickEvents.END_SERVER_TICK.register(server ->
		{
			if (server.isDedicatedServer())
				BootstrapChecks.serverTick(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server ->
		{
			if (server.isDedicatedServer())
				BootstrapChecks.serverStopped();
		});
	}
}
