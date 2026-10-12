/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;

import com.arcanc.pulselib.plugin.PulsePluginManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonEvents
{
	public static void registerCommonEvents(IEventBus bus)
	{
		bus.addListener(CommonEvents :: setup);
	}

	private static void setup(FMLCommonSetupEvent event)
	{
		event.enqueueWork(() -> PulsePluginManager.get().registerCommon());
	}
}
