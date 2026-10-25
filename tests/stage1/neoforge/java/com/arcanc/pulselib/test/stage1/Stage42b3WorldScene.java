/**
 * @author ArcAnc
 * Created at: 24.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;

/** Real owners and native terrain; never modify a save outside this named disposable fixture. */
public final class Stage42b3WorldScene
{
	private Stage42b3WorldScene()
	{
	}

	public static void prepare(ServerPlayer player)
	{
		prepare(player, "stage42b3-smoke");
	}

	public static void prepareTranslucent(ServerPlayer player)
	{
		prepare(player, "stage42b4-smoke");
	}

	public static void prepareLifecycle(ServerPlayer player)
	{
		prepare(player, "stage42c-smoke");
	}

	private static void prepare(ServerPlayer player, String worldName)
	{
		var server = player.level().getServer();
		if (!server.getWorldData().getLevelName().equals(worldName))
			throw new IllegalStateException("World overlap checks require the disposable " + worldName + " world");
		var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
		for (String command : new String[] {
				"kill @e[type=pulselib:test_entity]",
				"fill -12 79 -8 12 79 17 minecraft:stone",
				"fill -12 80 16 12 86 16 minecraft:white_concrete",
				"fill -9 79 11 -1 85 11 minecraft:blue_stained_glass",
				"fill 0 79 10 8 85 12 minecraft:glass",
				"fill 1 80 11 7 84 11 minecraft:water",
				"setblock -4 80 9 pulselib:test_block",
				"setblock -2 80 13 pulselib:test_block",
				"setblock 4 80 9 pulselib:test_block",
				"setblock 6 80 13 pulselib:test_block",
				"summon pulselib:test_entity -7.5 80 9.5 {NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b}",
				"summon pulselib:test_entity -5.5 80 13.5 {NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b}",
				"summon pulselib:test_entity 0.5 80 9.5 {NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b}",
				"summon pulselib:test_entity 2.5 80 13.5 {NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b}",
				"gamemode creative @s",
				"tp @s 0 80 -6 0 0",
				"time set noon",
				"weather clear" })
			server.getCommands().performPrefixedCommand(source, command);
	}
}
