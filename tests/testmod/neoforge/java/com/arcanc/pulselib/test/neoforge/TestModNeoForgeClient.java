/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.neoforge;

import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.player.PPlayerAcrobaticDemo;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import com.arcanc.pulselib.test.DemoClient;
import com.arcanc.pulselib.test.DemoContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

final class TestModNeoForgeClient
{
	static void register(IEventBus bus)
	{
		bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
		{
			event.registerEntityRenderer(DemoContent.entity.get(), TestEntityRender :: new);
			event.registerBlockEntityRenderer(DemoContent.blockEntity.get(), TestBlockEntityRenderer :: new);
		});
		bus.addListener((RegisterKeyMappingsEvent event) ->
		{
			event.register(PPlayerBallDemo.KEY);
			event.register(PPlayerAcrobaticDemo.KEY);
		});
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> DemoClient.tick());
	}
}
