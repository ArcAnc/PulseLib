/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap.neoforge;

import com.arcanc.pulselib.test.bootstrap.BootstrapChecks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(BootstrapChecks.MOD_ID)
public final class TestModNeoForge
{
	public TestModNeoForge(IEventBus bus)
	{
		BootstrapChecks.runDirectory();
		BootstrapChecks.jar(TestModNeoForge.class);
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
		{
			if (event.getServer().isDedicatedServer())
				BootstrapChecks.serverStarted(event.getServer());
		});
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) ->
		{
			if (event.getServer().isDedicatedServer())
				BootstrapChecks.serverTick(event.getServer());
		});
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
		{
			if (event.getServer().isDedicatedServer())
				BootstrapChecks.serverStopped();
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT)
			TestModNeoForgeClient.register();
	}
}
