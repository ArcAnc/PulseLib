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
import net.minecraft.world.level.gamerules.GameRules;

/** Native rain columns are confined to the front or back of the owner by real heightmap roofs. */
public final class Stage52AnimatedEffectsScene
{
	private Stage52AnimatedEffectsScene()
	{
	}

	public static void prepare(ServerPlayer player)
	{
		var server = player.level().getServer();
		if (!server.getWorldData().getLevelName().equals("animated-effects-smoke"))
			throw new IllegalStateException("Animated effects require their disposable named world");
		var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
		for (String command : new String[] {
				"gamerule minecraft:spawn_mobs false",
				"gamerule minecraft:advance_time false",
				"difficulty peaceful",
				"kill @e[type=!minecraft:player]",
				"kill @e[type=!minecraft:player]",
				"fill -10 79 -19 34 79 14 minecraft:stone",
				"fill -10 80 6 34 85 6 minecraft:sea_lantern",
				"fill -8 86 -1 8 86 14 minecraft:stone",
				"fill 16 86 -18 32 86 1 minecraft:stone",
				"gamemode creative @s",
				"tp @s 0.5 80 -3 0 0",
				"clear @s",
				"time set noon",
				"weather clear" })
			server.getCommands().performPrefixedCommand(source, command);
		if (server.getGameRules().get(GameRules.SPAWN_MOBS) || server.getGameRules().get(GameRules.ADVANCE_TIME))
			throw new IllegalStateException("Disposable effects scene must disable mob spawning and day progression");
	}
}
