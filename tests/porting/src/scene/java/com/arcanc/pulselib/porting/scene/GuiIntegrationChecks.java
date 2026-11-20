/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderState;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

final class GuiIntegrationChecks
{
	private GuiIntegrationChecks()
	{
	}

	static void run(Identifier texture)
	{
		var device = RenderSystem.getDevice();
		var dispatcher = Minecraft.getInstance().gameRenderer.featureRenderDispatcher();
		var parent = new TextureTarget("GUI parent target", 64, 64, true, GpuFormat.RGBA8_UNORM);
		var matrix = RenderSystem.getModelViewMatrixCopy();
		var state = new GuiEntityRenderState(new EntityRenderState(), new Vector3f(), new Quaternionf(), null, 0, 0, 32, 32, 1, null);
		try (var saved = new PRhiRenderState(); var pip = new DeferredPip(texture); var atlas = new GuiItemAtlas(dispatcher, 64, 32))
		{
			RenderSystem.outputColorTextureOverride = parent.getColorTextureView();
			RenderSystem.outputDepthTextureOverride = parent.getDepthTextureView();
			RenderSystem.enableScissorForRenderTypeDraws(40, 2, 20, 27);
			var projection = RenderSystem.getProjectionMatrixBuffer();
			var lights = RenderSystem.getShaderLights();
			try (var outer = PRenderContexts.enter(PRenderContexts.Context.OFFSCREEN))
			{
				var gui = new GuiRenderState();
				int initial = SceneChecks.instantCounters()[1];
				pip.abort = true;
				expectAbort(() -> pip.prepare(state, gui, dispatcher, 1));
				SceneChecks.check(SceneChecks.instantCounters()[1] == initial, "Aborted PIP submitted an incomplete queue");
				checkCaller(parent, matrix);
				pip.abort = false;
				pip.prepare(state, gui, dispatcher, 1);
				SceneChecks.check(SceneChecks.instantCounters()[1] == initial + 1, "Deferred feature was not drawn by PIP scope");
				SceneChecks.check(pip.coverage > 0, "Deferred PIP draw did not reach native attachment");
				checkCaller(parent, matrix);
				expectAbort(() -> atlas.getOrUpdate(item(texture, true)));
				SceneChecks.check(SceneChecks.instantCounters()[1] == initial + 1, "Aborted atlas submitted incomplete slot");
				checkCaller(parent, matrix);
				var slot = Objects.requireNonNull(atlas.getOrUpdate(item(texture, false)));
				SceneChecks.check(SceneChecks.instantCounters()[1] == initial + 2, "Special item missed the atlas queue");
				SceneChecks.check(coverage(slot.textureView().texture()) > 0, "Special item did not reach the atlas attachment");
				checkCaller(parent, matrix);
				SceneChecks.check(RenderSystem.getProjectionMatrixBuffer() == projection && RenderSystem.getShaderLights() == lights,
						"Nested GUI changed projection or lights of caller");
			}
		}
		finally
		{
			parent.destroyBuffers();
		}
		System.out.println("PASS native PIP/atlas deferred features, GPU coverage, nested target/scissor/lighting and failure recovery on " + device.getDeviceInfo().backendName());
	}

	private static void checkCaller(TextureTarget target, Matrix4f matrix)
	{
		var scissor = RenderSystem.getScissorStateForRenderTypeDraws();
		SceneChecks.check(PRenderContexts.current() == PRenderContexts.Context.OFFSCREEN &&
				RenderSystem.outputColorTextureOverride == target.getColorTextureView() &&
				RenderSystem.outputDepthTextureOverride == target.getDepthTextureView() &&
				RenderSystem.getModelViewStack().equals(matrix, 1e-6f) &&
				scissor.enabled() && scissor.x() == 40 && scissor.y() == 2 && scissor.width() == 20 && scissor.height() == 27,
				"Native GUI call leaked caller render state");
	}

	private static void expectAbort(Runnable render)
	{
		try
		{
			render.run();
			throw new AssertionError("Fixture did not abort");
		}
		catch (ExpectedGuiAbort expected)
		{
			SceneChecks.check(expected.getSuppressed().length == 0, "GUI cleanup threw");
		}
	}

	private static int coverage(GpuTexture texture)
	{
		try
		{
			return WorldReadback.read(texture, texture.getWidth(0) / 2, null).nonempty();
		}
		catch (Exception failure)
		{
			throw new IllegalStateException(failure);
		}
	}

	private static TrackingItemStackRenderState item(Identifier texture, boolean abort)
	{
		var item = new TrackingItemStackRenderState();
		item.appendModelIdentityElement(texture);
		item.newLayer().setupSpecialModel(new SpecialModelRenderer<Boolean>()
		{
			@Override
			public void submit(@Nullable Boolean fail, PoseStack poses, SubmitNodeCollector collector,
			                   int light, int overlay, boolean foil, int outline)
			{
				submitTriangle(texture, new Matrix4f(poses.last().pose()).scale(0.3f));
				if (Boolean.TRUE.equals(fail))
					throw new ExpectedGuiAbort();
			}

			@Override
			public void getExtents(Consumer<Vector3fc> output)
			{
				output.accept(new Vector3f());
			}

			@Override
			public Boolean extractArgument(ItemStack stack)
			{
				return abort;
			}
		}, abort);
		return item;
	}

	private static void submitTriangle(Identifier texture, Matrix4f transform)
	{
		SceneChecks.check(PRenderContexts.current() == PRenderContexts.Context.GUI, "GUI callback is outside the native slot scope");
		PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesInstantEmissiveSolid(texture),
				IntegrationChecks.GEOMETRY, new PInstanceHeader(transform, 0xFF00FF00, 0xF000F0, OverlayTexture.NO_OVERLAY));
	}

	private static final class DeferredPip extends PictureInPictureRenderer<GuiEntityRenderState>
	{
		private final Identifier texture;
		private boolean abort;
		private int coverage;

		private DeferredPip(Identifier texture)
		{
			this.texture = texture;
		}

		@Override
		protected void renderToTexture(GuiEntityRenderState state, PoseStack poses, SubmitNodeCollector collector)
		{
			collector.submitCustomGeometry(poses, RenderTypes.debugTriangleFan(), (pose, vertices) ->
			{
				submitTriangle(this.texture, new Matrix4f().translation(16, 16, 0).scale(8));
				if (this.abort)
					throw new ExpectedGuiAbort();
			});
		}

		@Override
		protected void blitTexture(GuiEntityRenderState state, GuiRenderState gui)
		{
			super.blitTexture(state, gui);
			gui.forEachElement(element ->
			{
				if (element instanceof BlitRenderState blit)
					this.coverage = coverage(Objects.requireNonNull(blit.textureSetup().texure0()).texture());
			}, GuiRenderState.TraverseRange.ALL);
		}

		@Override
		public Class<GuiEntityRenderState> getRenderStateClass()
		{
			return GuiEntityRenderState.class;
		}

		@Override
		protected String getTextureLabel()
		{
			return "Pulse deferred fixture";
		}
	}

	private static final class ExpectedGuiAbort extends RuntimeException
	{
	}
}
