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
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.Registries;

import java.util.List;

public final class FabricTestMod implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		for (var registry : List.of(Registries.BLOCK, Registries.BLOCK_ENTITY_TYPE, Registries.ENTITY_TYPE, Registries.ITEM))
			TestContent.register(registry);
		FabricDefaultAttributeRegistry.register(TestContent.creature(), TestCreature.createAttributes());
	}
}
