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
import com.arcanc.pulselib.test.stage62armor.ArmorProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class ArmorNeoForgeClient
{
	private ArmorNeoForgeClient()
	{
	}

	public static void register(IEventBus bus)
	{
		ArmorProbe.neo = true;
		bus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(new IClientItemExtensions()
		{
			@Override
			public Model<?> getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original)
			{
				return type == EquipmentClientInfo.LayerType.HUMANOID || type == EquipmentClientInfo.LayerType.HUMANOID_BABY ||
						type == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS ? ArmorProbe.extension(original) : original;
			}
		}, ArmorContent.ITEMS.toArray(Item[]::new)));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ArmorProbe.tick(net.minecraft.client.Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> ArmorProbe.shutdown());
	}
}
