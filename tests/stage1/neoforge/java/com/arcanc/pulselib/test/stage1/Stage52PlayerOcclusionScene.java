/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;

/** Dedicated native player / PulseLib entity scene, confined to a named disposable world. */
public final class Stage52PlayerOcclusionScene
{
	private Stage52PlayerOcclusionScene()
	{
	}

	public static void prepare(ServerPlayer player)
	{
		var server = player.level().getServer();
		if (!server.getWorldData().getLevelName().equals("player-occlusion-smoke"))
			throw new IllegalStateException("Player occlusion requires the disposable player-occlusion-smoke world");
		var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
		for (String command : new String[] {
				"kill @e[type=pulselib:test_entity]",
				"fill -8 79 -9 8 79 8 minecraft:stone",
				"fill -8 80 6 8 86 6 minecraft:sea_lantern",
				"summon pulselib:test_entity 0 80 2 {NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b}",
				"gamemode creative @s",
				"tp @s 0 80 -6 0 0",
				"clear @s",
				"time set noon",
				"weather clear" })
			server.getCommands().performPrefixedCommand(source, command);
	}
}
