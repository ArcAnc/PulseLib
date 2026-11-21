/**
 * @author ArcAnc
 * Created at: 21.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public final class FabricTestClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		EntityRenderers.register(TestContent.creature(), TestRenderers.Creature::new);
		BlockEntityRenderers.register(TestContent.blockEntity(), TestRenderers.Block::new);
		SpecialModelRenderers.ID_MAPPER.put(TestContent.BLOCK_ID, TestRenderers.UnbakedItem.CODEC);
	}
}
