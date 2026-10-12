package com.arcanc.pulselib.test.neoforge;

import com.arcanc.pulselib.test.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Ordinary game content registration. No PulseLib plugin registration. */
@Mod("pulselibtest")
public final class TestModNeoForge
{
	public TestModNeoForge(IEventBus bus)
	{
		var items = DeferredRegister.createItems("pulselibtest");
		var animated = items.register("animated", TestItem::new);
		var armor = items.register("armor", TestArmorItem::new);
		items.register(bus);
		bus.addListener((FMLCommonSetupEvent event) -> {
			TestItems.animated = animated.get();
			TestItems.armor = armor.get();
		});
	}
}
