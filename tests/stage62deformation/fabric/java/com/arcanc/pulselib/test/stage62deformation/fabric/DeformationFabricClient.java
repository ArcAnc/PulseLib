/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation.fabric;

import com.arcanc.pulselib.test.stage62deformation.DeformationProbe;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class DeformationFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		ClientTickEvents.END_CLIENT_TICK.register(DeformationProbe::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> DeformationProbe.shutdown());
	}
}
