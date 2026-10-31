/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61worldserver;

import com.arcanc.pulselib.util.PLibDatabase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;

/** Isolates the disposable lifecycle world from hostile mobs and survival death. */
public final class WorldScene implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
		{
			String worldName = Boolean.getBoolean("pulselib.fabricTransparencyProbe") ? "fabric-world-transparency" : "fabric-world-lifecycle";
			if (!server.getWorldData().getLevelName().equals(worldName))
				throw new IllegalStateException("Fabric world probe requires its disposable named world");
			for (String command : new String[] {"difficulty peaceful", "defaultgamemode creative",
					"gamerule minecraft:spawn_mobs false", "kill @e[type=!minecraft:player]"})
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
			if (server.getGameRules().get(GameRules.SPAWN_MOBS) || server.getDefaultGameType() != GameType.CREATIVE)
				throw new AssertionError("STAGE61B2B world isolation failed");
			if (Boolean.getBoolean("pulselib.fabricTransparencyProbe"))
				TransparencyScene.prepare(server);
			PLibDatabase.LOGGER.info("STAGE61B2B WORLD SCENE PASS: peaceful, creative, spawn_mobs=false");
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handler.player.setGameMode(GameType.CREATIVE));
	}
}
