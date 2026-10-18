/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric common entry point that initializes built-in types and common plugin registration.
 */
public final class PulseLibFabric implements ModInitializer
{
	/**
	 * Initializes built-in registries and invokes common plugin registration on Fabric.
	 */
	@Override
	public void onInitialize()
	{
		PLibRegistration.init();
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			ServerLifecycleEvents.SERVER_STARTING.register(server -> PulsePluginManager.get().registerCommon());
	}
}
