package com.arcanc.pulselib.test.fabric;

import com.arcanc.pulselib.test.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Ordinary game content registration. No PulseLib plugin registration. */
public final class TestModFabric implements ModInitializer
{
	@Override public void onInitialize()
	{
		TestItems.animated = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pulselibtest", "animated"), new TestItem());
		TestItems.armor = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pulselibtest", "armor"), new TestArmorItem());
	}
}
