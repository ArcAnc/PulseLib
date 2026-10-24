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
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Actual world hooks and main-target RGB/depth, with emissive, camera-relative control patches.
 * This verifies early opaque timing and ordinary alpha ordering, not the water/Fabulous OIT matrix.
 */
public final class Stage42aWorldStageProbe
{
	public static final Identifier WHITE = PLibDatabase.rl("stage42a_white");
	private static final RenderPipeline ALPHA_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET).
			withLocation(PLibDatabase.rl("stage42a/ordinary_alpha")).
			withVertexShader(PLibDatabase.rl("core/triangles")).
			withFragmentShader(PLibDatabase.rl("core/triangles")).
			withShaderDefine("EMISSIVE").
			withSampler("Sampler0").withSampler("Sampler1").withSampler("Sampler2").
			withUniform("DeformerOperations", UniformType.TEXEL_BUFFER, TextureFormat.RED8I).
			withUniform("DeformerValues", UniformType.TEXEL_BUFFER, TextureFormat.RED8I).
			withDepthStencilState(DepthStencilState.DEFAULT).
			withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).
			withVertexFormat(PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL, VertexFormat.Mode.TRIANGLES).
			withCull(false).build();
	private static final RenderType ALPHA = RenderType.create("stage42a_ordinary_alpha",
			RenderSetup.builder(ALPHA_PIPELINE).withTexture("Sampler0", PResourceCache.ATLAS_LOCATION).useOverlay().createRenderSetup());
	private static final PRenderQueue.RenderStage[] SOURCES = {PRenderQueue.RenderStage.ENTITIES,
			PRenderQueue.RenderStage.SOLID_BLOCKS, PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS};
	private static final float[] DEPTH = new float[4];
	private static int pending;
	private static int phase;
	private static int passes;

	private Stage42aWorldStageProbe()
	{
	}

	public static void register(IEventBus bus)
	{
		bus.addListener((RegisterRenderPipelinesEvent event) -> event.registerPipeline(ALPHA_PIPELINE));
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Stage42aWorldStageProbe::submit);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42aWorldStageProbe::early);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42aWorldStageProbe::features);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42aWorldStageProbe::terrain);
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Stage42aWorldStageProbe::lateSubmission);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42aWorldStageProbe::finish);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage42aWorldStageProbe::shutdown);
	}

	public static void tick(int tick)
	{
		// Separate frames from the 4.1b.3 probe that deliberately consumes its own opaque plans.
		if (tick == 110 || tick == 240)
			pending = tick;
	}

	private static void submit(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		if (pending == 0 || phase != 0)
			return;
		check(!PRenderTypes.usesOit(ALPHA) && PRenderTypes.isTransparent(ALPHA), "Control pipeline must use ordinary blending");
		PGeometryData quad = quad();
		RenderType solid = PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION);
		for (int patch = 0; patch < 4; patch++)
		{
			float distance = patch < 2 ? 0.3f : 0.7f;
			int color = patch == 0 ? 0xFFFF0000 : patch == 1 ? 0xFF00FF00 : 0xFF000000;
			PRenderQueue.submit(patch == 0 ? SOURCES[0] : SOURCES[1], solid, quad, header(patch, distance, color));
			Vector3f projected = new PGlRenderHost().captureFrame().projection().transformProject(new Vector3f(0, 0, -distance));
			DEPTH[patch] = (projected.z + 1) * 0.5f;
		}
		PRenderQueue.submitEntityMesh(ALPHA, quad, header(0, 0.4f, 0x800000FF));
		PRenderQueue.submitBlockEntityMesh(ALPHA, quad, header(1, 0.2f, 0x800000FF));
		// Submission and source order both disagree with back-to-front depth order.
		PRenderQueue.submitBlockEntityTranslucentMesh(ALPHA, quad, header(2, 0.4f, 0x800000FF));
		PRenderQueue.submitEntityMesh(ALPHA, quad, header(2, 0.6f, 0x80FF0000));
		PRenderQueue.submitBlockEntityMesh(ALPHA, quad, header(2, 0.5f, 0x8000FF00));
		PRenderQueue.submitEntityMesh(ALPHA, quad, header(3, 0.4f, 0x800000FF));
		PRenderQueue.submitBlockEntityTranslucentMesh(ALPHA, quad, header(3, 0.6f, 0x80FF0000));
		phase = 1;
	}

	private static void early(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		if (phase != 1)
			return;
		verifyOpaque("before native translucent depth copy");
		phase = 2;
	}

	private static void features(RenderLevelStageEvent.AfterTranslucentFeatures event)
	{
		if (phase != 2)
			return;
		verifyOpaque("after native translucent features");
		phase = 3;
	}

	private static void terrain(RenderLevelStageEvent.AfterTranslucentBlocks event)
	{
		if (phase != 3)
			return;
		verifyOpaque("after native translucent terrain");
		phase = 4;
	}

	private static void lateSubmission(RenderLevelStageEvent.AfterTranslucentParticles event)
	{
		if (phase != 4)
			return;
		PRenderQueue.submitBlockEntityMesh(ALPHA, quad(), header(3, 0.5f, 0x8000FF00));
		phase = 5;
	}

	private static void finish(RenderLevelStageEvent.AfterTranslucentParticles event)
	{
		if (phase != 5)
			return;
		float alpha = 128f / 255;
		float inverse = 1 - alpha;
		float[][] expected = {{1, 0, 0}, {0, inverse, alpha},
				{alpha * inverse * inverse, alpha * inverse, alpha}, {alpha * inverse * inverse, alpha * inverse, alpha}};
		for (int patch = 0; patch < 4; patch++)
			verify(patch, expected[patch], "after native translucent particles");
		PLibDatabase.LOGGER.info("STAGE 4.2a WORLD PASS: tick={}, early opaque Entity/BlockEntity RGB/depth; alpha retained through terrain; three-source global blend RGB={}, including late submission; no alpha depth writes",
				pending, Arrays.toString(pixel(2)));
		phase = 0;
		pending = 0;
		passes++;
	}

	private static void verifyOpaque(String boundary)
	{
		for (int patch = 0; patch < 4; patch++)
			verify(patch, patch == 0 ? new float[]{1, 0, 0} : patch == 1 ? new float[]{0, 1, 0} : new float[]{0, 0, 0}, boundary);
	}

	private static void verify(int patch, float[] expected, String boundary)
	{
		float[] actual = pixel(patch);
		for (int channel = 0; channel < 3; channel++)
			check(Math.abs(actual[channel] - expected[channel]) < 0.015f,
					boundary + " patch=" + patch + " expected RGB=" + Arrays.toString(expected) + " actual RGB/depth=" + Arrays.toString(actual));
		check(Math.abs(actual[3] - DEPTH[patch]) < 0.00001f,
				boundary + " patch=" + patch + " expected depth=" + DEPTH[patch] + " actual=" + actual[3]);
	}

	static PInstanceHeader header(int patch, float distance, int color)
	{
		PRenderFrame frame = new PGlRenderHost().captureFrame();
		Matrix4f inverseProjection = new Matrix4f(frame.projection()).invert();
		float z = frame.projection().transformProject(new Vector3f(0, 0, -distance)).z;
		Vector3f center = inverseProjection.transformProject(new Vector3f(-0.6f + patch * 0.4f, 0.6f, z));
		Vector3f right = inverseProjection.transformProject(new Vector3f(-0.54f + patch * 0.4f, 0.6f, z)).sub(center);
		Vector3f up = inverseProjection.transformProject(new Vector3f(-0.6f + patch * 0.4f, 0.66f, z)).sub(center);
		Matrix4f local = new Matrix4f().m00(right.x).m01(right.y).m02(right.z).
				m10(up.x).m11(up.y).m12(up.z).m30(center.x).m31(center.y).m32(center.z);
		return new PInstanceHeader(new Matrix4f(frame.modelView()).invert().mul(local), color, 0xF000F0, OverlayTexture.NO_OVERLAY);
	}

	static PGeometryData quad()
	{
		var sprite = PResourceCache.getTextureAtlas().getSprite(PResourceCache.spriteId(WHITE));
		check(sprite.contents().name().equals(PResourceCache.spriteId(WHITE)), "White control sprite must be present after each reload");
		float u = (sprite.getU0() + sprite.getU1()) * 0.5f;
		float v = (sprite.getV0() + sprite.getV1()) * 0.5f;
		int stride = PRenderTypes.VertexFormatProvider.POSITION_TEX_NORMAL.getVertexSize();
		ByteBuffer vertices = ByteBuffer.allocate(stride * 4).order(ByteOrder.nativeOrder());
		for (int i = 0; i < 4; i++)
		{
			int start = vertices.position();
			vertices.putFloat(i == 0 || i == 3 ? -1 : 1).putFloat(i < 2 ? -1 : 1).putFloat(0);
			vertices.putFloat(u).putFloat(v).put((byte)0).put((byte)0).put((byte)127);
			vertices.position(start + stride);
		}
		vertices.flip();
		ByteBuffer indices = ByteBuffer.allocate(12).order(ByteOrder.nativeOrder());
		for (int index : new int[]{0, 1, 2, 0, 2, 3})
			indices.putShort((short)index);
		indices.flip();
		return new PGeometryData(vertices, indices, stride, 6, PGeometryData.IndexType.UNSIGNED_SHORT);
	}

	/** Reads the real main attachments without changing Minecraft's draw framebuffer or cached state. */
	private static float[] pixel(int patch)
	{
		var target = Minecraft.getInstance().getMainRenderTarget();
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
					((GlTexture)target.getColorTexture()).glId(), 0);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D,
					((GlTexture)target.getDepthTexture()).glId(), 0);
			check(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Control read framebuffer must be complete");
			int x = (int)((0.2f + patch * 0.2f) * target.width);
			int y = (int)(0.8f * target.height);
			var rgba = BufferUtils.createFloatBuffer(4);
			var depth = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, rgba);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
			return new float[]{rgba.get(0), rgba.get(1), rgba.get(2), depth.get(0)};
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(passes == 2 && pending == 0 && phase == 0, "Both complete world-hook sequences must pass before/after reload");
		PLibDatabase.LOGGER.info("STAGE 4.2a WORLD SHUTDOWN PASS: both pre/post reload world RGB/depth sequences complete");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("Stage 4.2a world regression: " + message);
	}
}
