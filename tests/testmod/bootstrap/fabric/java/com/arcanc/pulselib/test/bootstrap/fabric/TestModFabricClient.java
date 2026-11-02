/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap.fabric;

import com.arcanc.pulselib.test.bootstrap.BootstrapClient;
import com.arcanc.pulselib.test.bootstrap.BootstrapChecks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class TestModFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		BootstrapChecks.jar(TestModFabricClient.class);
		ClientTickEvents.END_CLIENT_TICK.register(BootstrapClient::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> BootstrapClient.stopped());
	}
}
