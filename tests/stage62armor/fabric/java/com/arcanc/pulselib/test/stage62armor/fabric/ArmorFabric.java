/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62armor.fabric;

import com.arcanc.pulselib.test.stage62armor.ArmorContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.GameType;

public final class ArmorFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		for (int i = 0; i < ArmorContent.SLOTS.size(); i++)
			Registry.register(BuiltInRegistries.ITEM, ArmorContent.id(ArmorContent.SLOTS.get(i)), ArmorContent.ITEMS.get(i));
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
		{
			if (!server.getWorldData().getLevelName().equals("armor-provider-fixture"))
				throw new AssertionError("Disposable provider world required");
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handler.player.setGameMode(GameType.CREATIVE));
	}
}
