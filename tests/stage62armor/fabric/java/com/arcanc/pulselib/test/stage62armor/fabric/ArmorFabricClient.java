/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62armor.fabric;

import com.arcanc.pulselib.test.stage62armor.ArmorProbe;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class ArmorFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		ClientTickEvents.END_CLIENT_TICK.register(ArmorProbe::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ArmorProbe.shutdown());
	}
}
