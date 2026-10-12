package com.arcanc.pulselib.content.event;

import com.arcanc.pulselib.plugin.PulsePluginManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonEvents
{
	public static void registerCommonEvents(IEventBus bus)
	{
		bus.addListener(CommonEvents::setup);
	}

	private static void setup(FMLCommonSetupEvent event)
	{
		event.enqueueWork(() -> PulsePluginManager.get().registerCommon());
	}
}
