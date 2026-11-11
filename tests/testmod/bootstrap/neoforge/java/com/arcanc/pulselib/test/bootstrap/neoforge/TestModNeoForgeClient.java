/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap.neoforge;

import com.arcanc.pulselib.test.bootstrap.BootstrapClient;
import com.arcanc.pulselib.test.bootstrap.BootstrapChecks;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

public final class TestModNeoForgeClient
{
	private TestModNeoForgeClient()
	{
	}

	public static void register()
	{
		BootstrapChecks.jar(TestModNeoForgeClient.class);
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
		{
			ArmorExtensionProbe.tick(Minecraft.getInstance());
			BootstrapClient.tick(Minecraft.getInstance());
		});
		NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) ->
		{
			ArmorExtensionProbe.stopped();
			BootstrapClient.stopped();
		});
	}
}
