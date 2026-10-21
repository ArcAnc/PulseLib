/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.event.PulseLibEvents;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.PTestArmor;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Requires actual world submissions before requesting normal client shutdown. */
public final class Stage1Client
{
	private static int worldTicks;
	public static int entitySubmissions;
	public static int blockSubmissions;
	public static int itemSubmissions;

	private Stage1Client()
	{
	}

	public static void register(IEventBus bus)
	{
		bus.addListener(Stage1Client::renderers);
		bus.addListener(Stage1Client::resources);
		bus.addListener((RegisterSpecialModelRendererEvent event) ->
				event.register(PLibDatabase.rl("test_block"), TestBlockItemRenderer.Unbaked.MAP_CODEC));
		NeoForge.EVENT_BUS.addListener(Stage1Client::tick);
	}

	private static void renderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerBlockEntityRenderer(Stage1Content.BETypeReg.TEST_BLOCK_ENTITY.get(), TestBlockEntityRenderer::new);
		event.registerEntityRenderer(Stage1Content.EntityTypeReg.TEST_ENTITY.get(), TestEntityRender::new);
	}

	private static void resources(PulseLibEvents.RegisterResourceEvent event)
	{
		event.model(TestEntityRender.MODEL_DATA).
				texture("tube", TestEntityRender.TUBE).
				texture("sphere", TestEntityRender.SPHERE).
				texture("torus", TestEntityRender.TORUS).
				texture("0", TestEntityRender.ZERO).
				texture("armor/0", TestEntityRender.ARMOR);
		event.model(PTestArmor.MODEL_DATA).texture("0", TestEntityRender.ARMOR);
		event.model(TestBlockEntityRenderer.MODEL_DATA).
				texture("cube_texture", TestBlockEntityRenderer.CUBE).
				texture("torus_texture", TestBlockEntityRenderer.TORUS).
				texture("tube_texture", TestBlockEntityRenderer.TUBE).
				texture("pyramid_texture", TestBlockEntityRenderer.PYRAMID);
		event.model(TestBlockItemRenderer.MODEL_DATA).
				texture("pyramid", TestBlockItemRenderer.PYRAMID).
				texture("circle", TestBlockItemRenderer.CIRCLE);
	}

	private static void tick(ClientTickEvent.Post event)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
		{
			return;
		}
		if (++worldTicks == 100)
		{
			Screenshot.grab(minecraft.gameDirectory, "stage1-world.png", minecraft.getMainRenderTarget(), 1,
					message -> LogUtils.getLogger().info("{}", message.getString()));
		}
		if (worldTicks == 300)
		{
			if (entitySubmissions < 20 || blockSubmissions < 20 || itemSubmissions < 20)
			{
				throw new AssertionError("Missing world rendering: entity=" + entitySubmissions + ", block=" + blockSubmissions + ", item=" + itemSubmissions);
			}
			LogUtils.getLogger().info("STAGE 1.2 WORLD SMOKE: entity={}, block={}, item={}", entitySubmissions, blockSubmissions, itemSubmissions);
			minecraft.stop();
		}
	}
}
