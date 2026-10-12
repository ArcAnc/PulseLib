/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.fabric;

import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.player.PPlayerAcrobaticDemo;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import com.arcanc.pulselib.test.DemoClient;
import com.arcanc.pulselib.test.DemoContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class TestModFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		EntityRendererRegistry.register(DemoContent.entity.get(), TestEntityRender :: new);
		BlockEntityRendererRegistry.register(DemoContent.blockEntity.get(), TestBlockEntityRenderer :: new);
		KeyBindingHelper.registerKeyBinding(PPlayerBallDemo.KEY);
		KeyBindingHelper.registerKeyBinding(PPlayerAcrobaticDemo.KEY);
		ClientTickEvents.END_CLIENT_TICK.register(client -> DemoClient.tick());
	}
}
