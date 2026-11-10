/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import java.util.ArrayList;
import java.util.List;

/** MC26 extraction/submit execution boundary and real InventoryScreen → native PiP consumer. */
final class RenderRegressionEntities
{
	private RenderRegressionEntities()
	{
	}

	static void dispatch(TestEntity entity)
	{
		Minecraft mc = Minecraft.getInstance();
		var dispatcher = mc.getEntityRenderDispatcher();
		var features = mc.gameRenderer.getFeatureRenderDispatcher();
		try (var caller = new GlRenderState(); var scope = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
		{
			try
			{
				var state = dispatcher.extractEntity(entity, 1);
				state.lightCoords = 0xF000F0;
				state.shadowPieces.clear();
				state.outlineColor = 0;
				dispatcher.submit(state, new CameraRenderState(), 0, 0, 0, new PoseStack(), features.getSubmitNodeStorage());
				features.renderAllFeatures();
			}
			catch (RuntimeException | Error failure)
			{
				scope.discard();
				features.clearSubmitNodes();
				throw failure;
			}
		}
	}

	private static GuiEntityRenderState inventoryState(TestEntity entity)
	{
		Minecraft mc = Minecraft.getInstance();
		GuiRenderState gui = new GuiRenderState();
		InventoryScreen.extractEntityInInventoryFollowsMouse(new GuiGraphicsExtractor(mc, gui, 0, 0), 0, 0, 64, 64, 16, 0, 32, 32, entity);
		List<GuiEntityRenderState> states = new ArrayList<>();
		gui.forEachPictureInPicture(state -> states.add((GuiEntityRenderState)state));
		DemoChecks.check(states.size() == 1, "Actual InventoryScreen extracts one native entity PiP");
		return states.getFirst();
	}

	static void inventory(TestEntity entity, RenderTarget target)
	{
		Minecraft mc = Minecraft.getInstance();
		try (var renderer = new GuiEntityRenderer(mc.renderBuffers().bufferSource(), mc.getEntityRenderDispatcher()))
		{
			renderer.prepare(inventoryState(entity), new GuiRenderState(), 1);
			RenderRegressionTargets.copy(texture(renderer), target);
		}
	}

	static void failure(TestEntity entity)
	{
		Minecraft mc = Minecraft.getInstance();
		try (var renderer = new GuiEntityRenderer(mc.renderBuffers().bufferSource(), mc.getEntityRenderDispatcher()))
		{
			try
			{
				renderer.prepare(inventoryState(entity), new GuiRenderState(), 1);
			}
			catch (ReportedException failure)
			{
				DemoChecks.check(alpha(texture(renderer)) == 0, "Native failed PiP discards partially submitted Pulse geometry");
				throw failure;
			}
		}
	}

	private static GlTexture texture(GuiEntityRenderer renderer)
	{
		try
		{
			var field = PictureInPictureRenderer.class.getDeclaredField("texture");
			field.setAccessible(true);
			return (GlTexture)field.get(renderer);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new IllegalStateException("Cannot inspect native PiP output", exception);
		}
	}

	private static int alpha(GlTexture texture)
	{
		return RenderRegressionTargets.alpha(texture);
	}
}
