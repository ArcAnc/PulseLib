/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class IntegrationChecks
{
	static final PGeometryData GEOMETRY = SceneChecks.geometry(0);
	private static int frames;
	static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("pulselib_client_scene", "integration_white");
	private static boolean worldStarted;
	private static boolean entityScreen;
	private static boolean done;
	private static int initialDraws;

	private IntegrationChecks()
	{
	}

	public static void tick()
	{
		Minecraft client = Minecraft.getInstance();
		if (done || !client.isGameLoadFinished() || client.gui.overlay() != null)
			return;
		if (frames++ == 0)
		{
			var backend = RenderSystem.getDevice().getDeviceInfo().backendName();
			SceneChecks.check(backend.equalsIgnoreCase(System.getProperty("pulselib.scene.backend")), "Unexpected native backend: " + backend);
			initialDraws = SceneChecks.instantCounters()[1];
			SceneChecks.texture(client, TEXTURE.getPath(), -1);
			client.gui.setScreen(new FixtureScreen(null));
		}
		else if (frames == 5)
		{
			SceneChecks.check(SceneChecks.instantCounters()[1] > initialDraws, "Extracted Pulse PIP never drew through the instant backend");
			GuiIntegrationChecks.run(TEXTURE);
			WorldIntegrationChecks.start(client);
			worldStarted = true;
		}
		else if (worldStarted && !entityScreen && WorldIntegrationChecks.tick(client))
		{
			entityScreen = true;
			client.gui.setScreen(new FixtureScreen(WorldIntegrationChecks.extractGuiEntity(client)));
		}
		else if (entityScreen && WorldIntegrationChecks.finish(client))
		{
			try
			{
				Files.writeString(Path.of("pulselib-scene-result.txt"), "PASS\n");
			}
			catch (IOException failure)
			{
				throw new IllegalStateException(failure);
			}
			System.out.println("PASS empty world, native entity/block model draws, combined alpha order, recovered first-person feature, and GUI entity/model on " + RenderSystem.getDevice().getDeviceInfo().backendName());
			done = true;
			client.stop();
		}
	}

	private static final class FixtureScreen extends Screen
	{
		private final PModelData model;
		private final @Nullable EntityRenderState entity;

		private FixtureScreen(@Nullable EntityRenderState entity)
		{
			super(Component.literal("PulseLib native rendering fixture"));
			this.entity = entity;
			var bone = new PBakedBone("root", new Vector3f(), new Quaternionf(), List.of(), null,
					List.of(SceneChecks.baked(GEOMETRY, TEXTURE, false, true)));
			var baked = new PBakedModel(List.of(bone), Map.of());
			this.model = new PModelData(TEXTURE, PModelData.DEFAULT_MODEL_LOADER)
			{
				@Override
				public PBakedModel getModel()
				{
					return baked;
				}
			};
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partialTick)
		{
			drawModel(graphics);
			if (this.entity != null)
			{
				graphics.item(Items.DIAMOND_SWORD.getDefaultInstance(), 10, 10);
				graphics.entity(this.entity, 16, new Vector3f(), new Quaternionf(), null, 100, 20, 148, 68);
			}
		}

		@Override
		public boolean isPauseScreen()
		{
			return false;
		}

		private <T extends PAnimatable<T>> void drawModel(GuiGraphicsExtractor graphics)
		{
			PLibHelper.<T>renderModelInGui(graphics, this.model, List.of(), PRenderTypes.RenderTypeProvider::trianglesInstantSolid,
					60, 60, new Vector3f(12, 12, 1), 0xFF00FF00, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
		}
	}
}
