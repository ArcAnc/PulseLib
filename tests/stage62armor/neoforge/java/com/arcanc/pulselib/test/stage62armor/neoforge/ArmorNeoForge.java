/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62armor.neoforge;

import com.arcanc.pulselib.test.stage62armor.ArmorContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;

@Mod("pulselib_stage62armor")
public final class ArmorNeoForge
{
	public ArmorNeoForge(IEventBus bus)
	{
		bus.addListener((RegisterEvent event) -> event.register(Registries.ITEM, helper ->
		{
			for (int i = 0; i < ArmorContent.SLOTS.size(); i++)
				helper.register(ArmorContent.id(ArmorContent.SLOTS.get(i)), ArmorContent.ITEMS.get(i));
		}));
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
		{
			if (!event.getServer().getWorldData().getLevelName().equals("armor-provider-fixture"))
				throw new AssertionError("Disposable provider world required");
		});
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) ->
		{
			if (event.getEntity() instanceof ServerPlayer player)
				player.setGameMode(GameType.CREATIVE);
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT)
			ArmorNeoForgeClient.register(bus);
	}
}
