/**
 * @author ArcAnc
 * Created at: 24.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Analytic layered-alpha pixels through the original 26.1 OIT backend and explicit queue API. */
public final class Stage42b1OitProbe
{
	private static final PRenderQueue.RenderStage STAGE = new PRenderQueue.RenderStage("stage42b1_oit");
	private static int pending;
	private static int passes;
	private static int cases;

	private Stage42b1OitProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(Stage42b1OitProbe::render);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42b1OitProbe::shutdown);
	}

	public static void tick(int tick)
	{
		if (tick == 115 || tick == 245)
			pending = tick;
	}

	private static void render(RenderLevelStageEvent.AfterOpaqueBlocks event)
	{
		if (pending == 0)
			return;
		var projection = RenderSystem.getProjectionMatrixBuffer();
		var projectionType = RenderSystem.getProjectionType();
		var colorOverride = RenderSystem.outputColorTextureOverride;
		var depthOverride = RenderSystem.outputDepthTextureOverride;
		State state = new State();
		RenderSystem.getModelViewStack().pushMatrix().identity();
		try (ProjectionMatrixBuffer identity = new ProjectionMatrixBuffer("stage42b1 identity"))
		{
			RenderSystem.setProjectionMatrix(identity.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			verify("opaque-depth", 0.5f, true, List.of(new Surface(0.7f, 0x800000FF), new Surface(0.3f, 0x80FF0000)));
			verify("four-peeled-layers", 1, true, List.of(new Surface(0.3f, 0x8000FF00), new Surface(0.7f, 0x80FFFF00),
					new Surface(0.1f, 0x80FF0000), new Surface(0.5f, 0x800000FF)));
			verify("reversed-submission-order", 1, true, List.of(new Surface(0.5f, 0x800000FF), new Surface(0.1f, 0x80FF0000),
					new Surface(0.7f, 0x80FFFF00), new Surface(0.3f, 0x8000FF00)));
			verify("color-only-destination", 1, false, List.of(new Surface(0.4f, 0x8000FF00)));
			verify("zero-alpha", 1, true, List.of(new Surface(0.2f, 0x00FF0000)));
			passes++;
			PLibDatabase.LOGGER.info("STAGE 4.2b.1 OIT PASS: tick={}, cases={}, explicit opaque depth/destination, four real GPU peel layers, RGB/depth, override isolation, rejected-size recovery, composite idempotence/state", pending, cases);
			pending = 0;
		}
		finally
		{
			RenderSystem.outputColorTextureOverride = colorOverride;
			RenderSystem.outputDepthTextureOverride = depthOverride;
			RenderSystem.setProjectionMatrix(projection, projectionType);
			RenderSystem.getModelViewStack().popMatrix();
			state.restore();
		}
	}

	private static void verify(String name, float opaqueDepth, boolean destinationDepth, List<Surface> surfaces)
	{
		int width = pending < 200 ? 64 : 96;
		int height = width / 2;
		TextureTarget source = new TextureTarget("stage42b1 opaque", width, height, true);
		TextureTarget destination = new TextureTarget("stage42b1 destination", width, height, destinationDepth);
		TextureTarget decoy = new TextureTarget("stage42b1 unrelated override", width, height, true);
		TextureTarget invalid = new TextureTarget("stage42b1 wrong size", width / 2, height, true);
		var previousColor = RenderSystem.outputColorTextureOverride;
		var previousDepth = RenderSystem.outputDepthTextureOverride;
		State state = new State();
		try
		{
			clear(source, 0xFF334D66, opaqueDepth);
			clear(destination, 0xFF1A4080, 0.17f);
			clear(decoy, 0xFF00FFFF, 0.01f);
			clear(invalid, 0xFFFF00FF, 0.9f);
			float[] background = pixel(destination);
			float[] sourceBefore = pixel(source);
			float[] decoyBefore = pixel(decoy);
			float[] mainBefore = pixel(Minecraft.getInstance().getMainRenderTarget());
			RenderSystem.outputColorTextureOverride = decoy.getColorTextureView();
			RenderSystem.outputDepthTextureOverride = decoy.getDepthTextureView();
			var geometry = Stage42aWorldStageProbe.quad();
			var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
			for (Surface surface : surfaces)
				PRenderQueue.submit(STAGE, type, geometry, new PInstanceHeader(new Matrix4f().translation(0, 0, surface.depth() * 2 - 1),
						surface.color(), 0xF000F0, OverlayTexture.NO_OVERLAY));
			var projection = RenderSystem.getProjectionMatrixBuffer();
			PRenderQueue.flush(STAGE, source);
			check(RenderSystem.getProjectionMatrixBuffer() == projection, name + " projection binding changed");
			check(Arrays.equals(background, pixel(destination)), name + " destination written before explicit composition");
			check(Arrays.equals(sourceBefore, pixel(source)), name + " opaque source changed during accumulation");
			check(Arrays.equals(decoyBefore, pixel(decoy)), name + " output override used instead of explicit depth source");
			check(Arrays.equals(mainBefore, pixel(Minecraft.getInstance().getMainRenderTarget())), name + " native world target changed");
			State beforeInvalid = new State();
			try
			{
				PRenderQueue.compositeTranslucency(invalid);
				throw new AssertionError(name + " mismatched destination must be rejected");
			}
			catch (IllegalArgumentException expected)
			{
				check(Arrays.equals(beforeInvalid.values, new State().values), name + " rejected target changed caller state");
			}
			// Deliberately unusual caller state: composition must restore it and ignore its scissor/depth test.
			GlStateManager._viewport(2, 3, 5, 7);
			GlStateManager._enableScissorTest();
			GlStateManager._scissorBox(0, 0, 1, 1);
			GlStateManager._enableDepthTest();
			GlStateManager._depthFunc(GL11.GL_GREATER);
			GlStateManager._depthMask(true);
			GlStateManager._enableBlend();
			GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ZERO, GL11.GL_ONE);
			State caller = new State();
			PRenderQueue.compositeTranslucency(destination);
			check(Arrays.equals(caller.values, new State().values), name + " composite changed caller framebuffer/viewport/depth/scissor/blend state");
			float[] actual = pixel(destination);
			float[] expected = reference(background, opaqueDepth, surfaces);
			for (int channel = 0; channel < 4; channel++)
				check(Math.abs(actual[channel] - expected[channel]) < 0.016f,
						name + " expected RGBA=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
			check(!destinationDepth || actual[4] == background[4], name + " composition wrote destination depth");
			check(Arrays.equals(sourceBefore, pixel(source)), name + " composition wrote opaque source");
			check(Arrays.equals(decoyBefore, pixel(decoy)), name + " composition wrote unrelated override target");
			check(RenderSystem.outputColorTextureOverride == decoy.getColorTextureView() &&
					RenderSystem.outputDepthTextureOverride == decoy.getDepthTextureView(), name + " changed override objects");
			PRenderQueue.compositeTranslucency(destination);
			check(Arrays.equals(actual, pixel(destination)), name + " second composite blended layers twice");
			PLibDatabase.LOGGER.info("STAGE 4.2b.1 OIT CASE PASS: tick={}, {}, {}x{}, opaqueDepth={}, RGBA/depth={}", pending, name, width, height, opaqueDepth, Arrays.toString(actual));
			cases++;
		}
		finally
		{
			RenderSystem.outputColorTextureOverride = previousColor;
			RenderSystem.outputDepthTextureOverride = previousDepth;
			source.destroyBuffers();
			destination.destroyBuffers();
			decoy.destroyBuffers();
			invalid.destroyBuffers();
			state.restore();
		}
	}

	private static float[] reference(float[] background, float depth, List<Surface> surfaces)
	{
		float[] result = Arrays.copyOf(background, 4);
		for (Surface surface : surfaces.stream().filter(surface -> surface.depth() <= depth).
				sorted(Comparator.comparingDouble(Surface::depth).reversed()).toList())
		{
			float alpha = (surface.color() >>> 24) / 255f;
			for (int channel = 0; channel < 3; channel++)
				result[channel] = ((surface.color() >>> (16 - channel * 8)) & 255) / 255f * alpha + result[channel] * (1 - alpha);
			result[3] = alpha + result[3] * (1 - alpha);
		}
		return result;
	}

	static void clear(RenderTarget target, int color, float depth)
	{
		var encoder = RenderSystem.getDevice().createCommandEncoder();
		if (target.useDepth)
			encoder.clearColorAndDepthTextures(target.getColorTexture(), color, target.getDepthTexture(), depth);
		else
			encoder.clearColorTexture(target.getColorTexture(), color);
	}

	static float[] pixel(RenderTarget target)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int framebuffer = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			if (target.useDepth)
				GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			check(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Read framebuffer incomplete");
			var color = BufferUtils.createFloatBuffer(4);
			var depth = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels(target.width / 2, target.height / 2, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, color);
			if (target.useDepth)
				GL11.glReadPixels(target.width / 2, target.height / 2, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
			return new float[]{color.get(0), color.get(1), color.get(2), color.get(3), target.useDepth ? depth.get(0) : -1};
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(framebuffer);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(passes == 2 && cases == 10 && pending == 0, "Both pre/post reload OIT suites must finish");
		PLibDatabase.LOGGER.info("STAGE 4.2b.1 OIT SHUTDOWN PASS: 10 GPU pixel cases, both complete suites");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 4.2b.1 OIT: " + message);
	}

	private record Surface(float depth, int color)
	{
	}

	/** Caller state which composition promises to preserve; no production snapshot helper is used. */
	static final class State
	{
		final int[] values = new int[19];

		State()
		{
			GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.values);
			int[] box = new int[4];
			GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
			System.arraycopy(box, 0, this.values, 4, 4);
			this.values[8] = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
			this.values[9] = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
			this.values[10] = GL11.glIsEnabled(GL11.GL_DEPTH_TEST) ? 1 : 0;
			this.values[11] = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) ? 1 : 0;
			this.values[12] = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
			this.values[13] = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) ? 1 : 0;
			this.values[14] = GL11.glIsEnabled(GL11.GL_BLEND) ? 1 : 0;
			this.values[15] = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
			this.values[16] = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
			this.values[17] = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
			this.values[18] = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		}

		void restore()
		{
			GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.values[8]);
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.values[9]);
			GlStateManager._viewport(this.values[0], this.values[1], this.values[2], this.values[3]);
			GlStateManager._scissorBox(this.values[4], this.values[5], this.values[6], this.values[7]);
			if (this.values[10] != 0)
				GlStateManager._enableDepthTest();
			else
				GlStateManager._disableDepthTest();
			GlStateManager._depthMask(this.values[11] != 0);
			GlStateManager._depthFunc(this.values[12]);
			if (this.values[13] != 0)
				GlStateManager._enableScissorTest();
			else
				GlStateManager._disableScissorTest();
			if (this.values[14] != 0)
				GlStateManager._enableBlend();
			else
				GlStateManager._disableBlend();
			GlStateManager._blendFuncSeparate(this.values[15], this.values[16], this.values[17], this.values[18]);
		}
	}
}
