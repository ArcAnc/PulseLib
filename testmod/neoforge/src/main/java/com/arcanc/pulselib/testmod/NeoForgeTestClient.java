/**
 * @author ArcAnc
 * Created at: 21.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(modid = TestContent.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeTestClient
{
	private NeoForgeTestClient()
	{
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerEntityRenderer(TestContent.creature(), TestRenderers.Creature::new);
		event.registerBlockEntityRenderer(TestContent.blockEntity(), TestRenderers.Block::new);
	}

	@SubscribeEvent
	public static void registerItemRenderer(RegisterSpecialModelRendererEvent event)
	{
		event.register(TestContent.BLOCK_ID, TestRenderers.UnbakedItem.CODEC);
	}
}
