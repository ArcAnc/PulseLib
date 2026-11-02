/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation.neoforge;

import com.arcanc.pulselib.test.stage62deformation.DeformationProbe;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

public final class DeformationNeoForgeClient
{
	private DeformationNeoForgeClient()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> DeformationProbe.tick(Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> DeformationProbe.shutdown());
	}
}
