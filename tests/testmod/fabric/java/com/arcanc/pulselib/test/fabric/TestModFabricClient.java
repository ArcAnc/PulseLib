package com.arcanc.pulselib.test.fabric;

import com.arcanc.pulselib.test.*;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.player.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class TestModFabricClient implements ClientModInitializer
{
	@Override public void onInitializeClient()
	{
		EntityRendererRegistry.register(DemoContent.entity.get(), TestEntityRender::new);
		BlockEntityRendererRegistry.register(DemoContent.blockEntity.get(), TestBlockEntityRenderer::new);
		KeyBindingHelper.registerKeyBinding(PPlayerBallDemo.KEY);
		KeyBindingHelper.registerKeyBinding(PPlayerAcrobaticDemo.KEY);
		ClientTickEvents.END_CLIENT_TICK.register(client -> DemoClient.tick());
	}
}
