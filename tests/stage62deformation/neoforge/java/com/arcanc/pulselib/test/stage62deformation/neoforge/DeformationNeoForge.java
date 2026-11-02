/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod("pulselib_stage62deformation")
public final class DeformationNeoForge
{
	public DeformationNeoForge(IEventBus bus)
	{
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
		{
			if (!"deformation-fixture".equals(event.getServer().getWorldData().getLevelName()))
				throw new AssertionError("Disposable deformation server world name");
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT)
			DeformationNeoForgeClient.register();
	}
}
