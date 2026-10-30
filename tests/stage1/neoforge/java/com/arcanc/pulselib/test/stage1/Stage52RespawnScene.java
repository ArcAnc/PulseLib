/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Explicit opt-in command exercises native server respawn of the connected player. */
public final class Stage52RespawnScene
{
	private static int respawns;

	private Stage52RespawnScene()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
				event.getDispatcher().register(Commands.literal("pulse_respawn_probe").executes(context ->
				{
					var player = context.getSource().getPlayerOrException();
					var server = context.getSource().getServer();
					var replacement = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
					if (replacement == player || !replacement.getUUID().equals(player.getUUID()) ||
							server.getPlayerList().getPlayer(player.getUUID()) != replacement ||
							replacement.connection != player.connection || !player.isRemoved())
						throw new AssertionError("STAGE525B native server respawn did not replace the connected owner");
					PLibDatabase.LOGGER.info("STAGE525B SERVER RESPAWN PASS: count={}, uuid={}, oldId={}, newId={}",
							++respawns, replacement.getUUID(), player.getId(), replacement.getId());
					return 1;
				})));
	}
}
