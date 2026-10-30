/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;

/** Three isolated rays: stained glass, a contained water volume and the identical clear control. */
public final class Stage52AnimatedEnvironmentScene
{
	private Stage52AnimatedEnvironmentScene()
	{
	}

	public static void prepare(ServerPlayer player)
	{
		var server = player.level().getServer();
		if (!server.getWorldData().getLevelName().equals("animated-environment-smoke"))
			throw new IllegalStateException("Animated environment requires its disposable named world");
		var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
		for (String command : new String[] {
				"kill @e[type=pulselib:test_entity]",
				"fill -10 79 -9 18 79 9 minecraft:stone",
				"fill -10 80 8 18 86 8 minecraft:sea_lantern",
				"fill -6 80 1 -2 85 1 minecraft:blue_stained_glass",
				"fill 2 79 1 6 85 3 minecraft:glass",
				"fill 3 80 2 5 84 2 minecraft:water",
				"gamemode creative @s",
				"tp @s 12.5 80 -6 0 0",
				"clear @s",
				"time set noon",
				"weather clear" })
			server.getCommands().performPrefixedCommand(source, command);
	}
}
