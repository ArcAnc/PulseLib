package com.arcanc.pulselib.test.neoforge;

import com.arcanc.pulselib.test.*;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.player.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;

final class TestModNeoForgeClient
{
	static void register(IEventBus bus)
	{
		bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
			event.registerEntityRenderer(DemoContent.entity.get(), TestEntityRender::new);
			event.registerBlockEntityRenderer(DemoContent.blockEntity.get(), TestBlockEntityRenderer::new);
		});
		bus.addListener((RegisterKeyMappingsEvent event) -> {
			event.register(PPlayerBallDemo.KEY);
			event.register(PPlayerAcrobaticDemo.KEY);
		});
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> DemoClient.tick());
	}
}
