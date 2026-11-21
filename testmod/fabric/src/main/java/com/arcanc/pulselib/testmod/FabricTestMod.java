/**
 * @author ArcAnc
 * Created at: 21.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.Registries;

public final class FabricTestMod implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		TestContent.register(Registries.BLOCK);
		TestContent.registerBlockEntity(block -> FabricBlockEntityTypeBuilder.create(TestBlockEntity::new, block).build());
		TestContent.register(Registries.ENTITY_TYPE);
		TestContent.register(Registries.ITEM);
		FabricDefaultAttributeRegistry.register(TestContent.creature(), TestCreature.createAttributes());
	}
}
