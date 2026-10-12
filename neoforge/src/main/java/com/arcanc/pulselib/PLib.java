/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib;

import com.arcanc.pulselib.content.event.ClientEvents;
import com.arcanc.pulselib.content.event.CommonEvents;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.util.PLibDatabase;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

@Mod(PLibDatabase.MOD_ID)
public class PLib
{
    public PLib(IEventBus modEventBus, ModContainer modContainer)
    {
	    PLibRegistration.init(modEventBus);
	    
	    setupEvents(modEventBus);
    }
	
	private void setupEvents(final IEventBus modEventBus)
	{
		CommonEvents.registerCommonEvents(modEventBus);
		if (FMLLoader.getDist().isClient())
			ClientEvents.registerClientEvents(modEventBus);
	}
}
