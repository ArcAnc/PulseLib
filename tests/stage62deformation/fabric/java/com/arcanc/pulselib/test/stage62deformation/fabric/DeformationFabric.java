/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class DeformationFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
		{
			if (!"deformation-fixture".equals(server.getWorldData().getLevelName()))
				throw new AssertionError("Disposable deformation server world name");
		});
	}
}
