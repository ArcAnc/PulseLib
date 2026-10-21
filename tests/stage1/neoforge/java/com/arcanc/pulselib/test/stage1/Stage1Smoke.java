/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.registration.entity.TestEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in original-content smoke scene. Only writes to the isolated run world. */
@Mod("pulselib_stage1")
public final class Stage1Smoke
{
	public Stage1Smoke(IEventBus bus, ModContainer container)
	{
		Stage1Content.register(bus);
		bus.addListener((EntityAttributeCreationEvent event) ->
				event.put(Stage1Content.EntityTypeReg.TEST_ENTITY.get(), TestEntity.createAttributes().build()));
		NeoForge.EVENT_BUS.addListener(Stage1Smoke::joined);
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
		{
			if (Boolean.getBoolean("pulselib.stage2Probe"))
			{
				Stage2PluginProbe.verify(false);
			}
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT)
		{
			Stage1Client.register(bus);
		}
	}

	private static void joined(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (!(event.getEntity() instanceof ServerPlayer player))
		{
			return;
		}
		var server = player.level().getServer();
		var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
		for (String command : new String[] {
				"kill @e[type=pulselib:test_entity]",
				"fill -8 79 -8 8 79 8 minecraft:stone",
				"tp @s 0 80 5 0 10",
				"gamemode creative @s",
				"time set noon",
				"weather clear",
				"setblock -2 80 9 pulselib:test_block",
				"setblock 2 80 9 pulselib:test_block",
				"summon pulselib:test_entity -1 80 10 {NoAI:1b,PersistenceRequired:1b}",
				"summon pulselib:test_entity 1 80 10 {NoAI:1b,PersistenceRequired:1b}",
				"give @s pulselib:test_block 1" })
		{
			server.getCommands().performPrefixedCommand(source, command);
		}
	}
}
