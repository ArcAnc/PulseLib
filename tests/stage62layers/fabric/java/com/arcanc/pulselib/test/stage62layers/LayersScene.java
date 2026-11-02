/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62layers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.level.GameType;

/** Refuses any save other than this checkpoint's own disposable world. */
public final class LayersScene implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
		{
			if (!server.getWorldData().getLevelName().equals("fabric-living-layers"))
				throw new AssertionError("Native layer fixture requires its disposable named world");
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handler.player.setGameMode(GameType.CREATIVE));
	}
}
