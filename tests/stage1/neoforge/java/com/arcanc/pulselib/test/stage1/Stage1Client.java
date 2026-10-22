/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.PTestArmor;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.platform.PulseClientServices;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/** Requires actual world submissions before requesting normal client shutdown. */
@PulsePlugin
public final class Stage1Client implements IPulseClientPlugin
{
	private static int worldTicks;
	private static @Nullable CompletableFuture<Void> reload;
	private static int entitiesBeforeReload;
	private static int blocksBeforeReload;
	private static int itemsBeforeReload;
	private static boolean reloadScreenshot;
	public static int entitySubmissions;
	public static int blockSubmissions;
	public static int itemSubmissions;

	public Stage1Client()
	{
		Stage2PluginProbe.clientConstructions++;
	}

	@Override
	public Identifier getPluginId()
	{
		return PLibDatabase.rl("stage1_client");
	}

	public static void register(IEventBus bus)
	{
		bus.addListener(Stage1Client::renderers);
		bus.addListener((RegisterSpecialModelRendererEvent event) ->
				event.register(PLibDatabase.rl("test_block"), TestBlockItemRenderer.Unbaked.MAP_CODEC));
		NeoForge.EVENT_BUS.addListener(Stage1Client::tick);
		if (Boolean.getBoolean("pulselib.stage41Probe"))
			Stage41PlanProbe.register();
		if (Boolean.getBoolean("pulselib.stage31Probe"))
		{
			NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage31ItemIdentityProbe::shutdown);
		}
	}

	private static void renderers(EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerBlockEntityRenderer(Stage1Content.BETypeReg.TEST_BLOCK_ENTITY.get(), context ->
		{
			var renderer = new TestBlockEntityRenderer(context);
			Stage32TimeProbe.blockRenderer = renderer;
			return renderer;
		});
		event.registerEntityRenderer(Stage1Content.EntityTypeReg.TEST_ENTITY.get(), context ->
		{
			var renderer = new TestEntityRender(context);
			Stage32TimeProbe.entityRenderer = renderer;
			return renderer;
		});
	}

	@Override
	public void registerModels(PModelRegistrar event)
	{
		Stage2PluginProbe.models++;
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

	@Override
	public void registerAttachments(PAttachmentRegistrar registrar)
	{
		Stage2PluginProbe.attachments++;
	}

	@Override
	public void registerPlayerAnimations(PPlayerAnimationRegistrar registrar)
	{
		Stage2PluginProbe.playerAnimations++;
	}

	@Override
	public void registerAnimatedAttachments(PAnimatedAttachmentRegistrar registrar)
	{
		Stage2PluginProbe.animatedAttachments++;
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
		boolean pluginProbe = Boolean.getBoolean("pulselib.stage2Probe");
		if (Boolean.getBoolean("pulselib.stage41Probe"))
			Stage41PlanProbe.tick(worldTicks);
		if (Boolean.getBoolean("pulselib.stage33Probe"))
		{
			Stage33GraphProbe.tick(worldTicks);
		}
		if (Boolean.getBoolean("pulselib.stage32Probe") && (worldTicks == 100 || worldTicks == 210))
		{
			Stage32TimeProbe.lighting(worldTicks);
		}
		if (Boolean.getBoolean("pulselib.stage31Probe") && worldTicks == 75)
		{
			Stage31ItemIdentityProbe.verify();
		}
		if (Boolean.getBoolean("pulselib.stage32Probe") && worldTicks == 80)
		{
			try
			{
				Stage32TimeProbe.verify();
			}
			catch (ReflectiveOperationException exception)
			{
				throw new AssertionError("Renderer Molang time probe failed", exception);
			}
		}
		if (pluginProbe && worldTicks == 50)
		{
			verifyCameraService(minecraft);
		}
		if (pluginProbe && worldTicks == 120)
		{
			entitiesBeforeReload = entitySubmissions;
			blocksBeforeReload = blockSubmissions;
			itemsBeforeReload = itemSubmissions;
			reload = minecraft.reloadResourcePacks();
		}
		if (pluginProbe && !reloadScreenshot && worldTicks >= 200 && reload != null && reload.isDone())
		{
			reload.join();
			Screenshot.grab(minecraft.gameDirectory, "stage2-after-reload.png", minecraft.getMainRenderTarget(), 1,
					message -> LogUtils.getLogger().info("{}", message.getString()));
			reloadScreenshot = true;
		}
		if (worldTicks > 1200)
		{
			throw new AssertionError("Stage 2 resource reload did not finish in time");
		}
		if (worldTicks >= 300 && (!pluginProbe || reload != null && reload.isDone()))
		{
			if (pluginProbe)
			{
				reload.join();
				Stage2PluginProbe.verify(true);
				if (entitySubmissions - entitiesBeforeReload < 20 || blockSubmissions - blocksBeforeReload < 20 ||
						itemSubmissions - itemsBeforeReload < 20 || !reloadScreenshot)
				{
					throw new AssertionError("Rendering did not recover after the plugin model reload");
				}
			}
			if (entitySubmissions < 20 || blockSubmissions < 20 || itemSubmissions < 20)
			{
				throw new AssertionError("Missing world rendering: entity=" + entitySubmissions + ", block=" + blockSubmissions + ", item=" + itemSubmissions);
			}
			LogUtils.getLogger().info("STAGE 1.2 WORLD SMOKE: entity={}, block={}, item={}", entitySubmissions, blockSubmissions, itemSubmissions);
			minecraft.stop();
		}
	}

	/** Exercises the real loader camera provider, including roll, then restores the ordinary camera. */
	private static void verifyCameraService(Minecraft minecraft)
	{
		var camera = minecraft.gameRenderer.getMainCamera();
		float yaw = camera.yRot();
		float pitch = camera.xRot();
		try
		{
			PulseClientServices.CAMERA.setRotation(camera, yaw, pitch, 17.0f);
			Quaternionf expected = new Quaternionf().rotationYXZ((float)Math.PI - (float)Math.toRadians(yaw),
					-(float)Math.toRadians(pitch), -(float)Math.toRadians(17.0f));
			Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(expected);
			if (Math.abs(1.0f - Math.abs(expected.dot(camera.rotation()))) > 0.00001f ||
					up.distanceSquared(camera.upVector()) > 0.00001f)
			{
				throw new AssertionError("Camera service did not update its orientation and basis vectors");
			}
		}
		finally
		{
			PulseClientServices.CAMERA.setRotation(camera, yaw, pitch, 0.0f);
		}
		LogUtils.getLogger().info("STAGE 2 CAMERA SERVICE PASS: {}", PulseClientServices.CAMERA.getClass().getName());
	}
}
