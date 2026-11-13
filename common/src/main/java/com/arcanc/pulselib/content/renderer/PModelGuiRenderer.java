/**
 * @author ArcAnc
 * Created at: 25.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jetbrains.annotations.ApiStatus;

/** Native PIP adapter; all meshes still execute through the shared Pulse compiler and GPU backend. */
@ApiStatus.Internal
public final class PModelGuiRenderer extends PictureInPictureRenderer<PModelGuiRenderState>
{
	public PModelGuiRenderer()
	{
	}

	/** Compatibility overload using the client's native dispatcher. */
	public void prepare(PModelGuiRenderState state, GuiRenderState gui, int guiScale)
	{
		prepare(state, gui, Minecraft.getInstance().gameRenderer.featureRenderDispatcher(), guiScale);
	}

	@Override
	public void prepare(PModelGuiRenderState state, GuiRenderState gui, FeatureRenderDispatcher dispatcher, int guiScale)
	{
		var lights = RenderSystem.getShaderLights();
		try
		{
			Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
			super.prepare(state, gui, dispatcher, guiScale);
		}
		finally
		{
			RenderSystem.setShaderLights(lights);
		}
	}

	@Override
	protected void renderToTexture(PModelGuiRenderState state, PoseStack poses, SubmitNodeCollector collector)
	{
		renderToTexture(state, poses);
	}

	/** Retains the canonical model traversal independently of native feature collection. */
	protected void renderToTexture(PModelGuiRenderState state, PoseStack poses)
	{
		// Cancel the native PIP anchor/Z reflection; the snapshot already contains GUI pose, origin and scale.
		poses.translate(-(state.x0() + state.x1()) / 2.0f, -state.y1(), 0);
		poses.scale(1, 1, -1);
		poses.mulPose(state.transform());
		for (var bone : state.model().bones())
			bone.instantDraw(poses, state.model(), state.animationPose(), state.renderType(), state.color(), state.packedLight(), state.packedOverlay());
	}

	@Override
	public Class<PModelGuiRenderState> getRenderStateClass()
	{
		return PModelGuiRenderState.class;
	}

	@Override
	protected String getTextureLabel()
	{
		return "Pulse model";
	}
}
