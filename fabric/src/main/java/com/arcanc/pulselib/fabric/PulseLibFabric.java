package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class PulseLibFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		PLibRegistration.init();
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			ServerLifecycleEvents.SERVER_STARTING.register(server -> PulsePluginManager.get().registerCommon());
	}
}
