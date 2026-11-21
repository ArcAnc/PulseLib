/**
 * @author ArcAnc
 * Created at: 21.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(TestContent.MOD_ID)
public final class NeoForgeTestMod
{
	public NeoForgeTestMod(IEventBus bus)
	{
		bus.addListener(NeoForgeTestMod::registerContent);
		bus.addListener((EntityAttributeCreationEvent event) -> event.put(TestContent.creature(), TestCreature.createAttributes().build()));
	}

	private static void registerContent(RegisterEvent event)
	{
		var registry = event.getRegistryKey();
		if (registry.equals(Registries.BLOCK_ENTITY_TYPE))
			TestContent.registerBlockEntity(block -> new BlockEntityType<>(TestBlockEntity::new, block));
		else
			TestContent.register(registry);
	}
}
