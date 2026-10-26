/**
 * @author ArcAnc
 * Created at: 26.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;

/** Server-only setup of the named disposable vanilla-owner scene. */
public final class Stage43cItemScene
{
	private Stage43cItemScene()
	{
	}

	public static void prepare(ServerPlayer player)
	{
		var server = player.level().getServer();
		if (!server.getWorldData().getLevelName().equals("stage43c-smoke"))
			throw new IllegalStateException("Standalone item probe requires its new disposable world");
		var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
		for (String command : new String[] {
				"forceload add -8 -8 8 16",
				"kill @e[type=pulselib:test_entity]",
				"fill -8 80 -8 8 85 16 minecraft:air replace pulselib:test_block",
				"kill @e[type=minecraft:item]", "kill @e[type=minecraft:armor_stand]",
				"fill -8 79 -8 8 79 16 minecraft:stone", "fill -8 80 15 8 85 15 minecraft:white_concrete",
				"fill -6 80 11 6 84 11 minecraft:blue_stained_glass",
				"summon minecraft:item -2 81 9 {NoGravity:1b,Age:-32768s,PickupDelay:32767s,Item:{id:\"pulselib:test_block\",count:1}}",
				"summon minecraft:item 2 81 13 {NoGravity:1b,Age:-32768s,PickupDelay:32767s,Item:{id:\"pulselib:test_block\",count:1}}",
				"summon minecraft:armor_stand 0 80 9 {NoGravity:1b,ShowArms:1b}",
				"item replace entity @e[type=minecraft:armor_stand,limit=1] weapon.mainhand with pulselib:test_block",
				"gamemode creative @s", "tp @s 0 80 5 0 0", "time set noon", "weather clear",
				"give @s pulselib:test_block[minecraft:enchantment_glint_override=true]" })
			server.getCommands().performPrefixedCommand(source, command);
	}
}
