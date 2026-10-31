/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client;

import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderFrame;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Native Fabric stage routing and main-target RGB/depth before and after resource reload. */
public final class WorldStageProbe
{
	private static final Identifier WHITE = PLibDatabase.rl("lifecycle/stage62a_white");
	private static final PModelData DATA = PModelData.direct(PLibDatabase.rl("lifecycle/atlas_gecko_0"), PGeckoModelLoader.INSTANCE.id());
	private static final Identifier BEFORE = PLibDatabase.rl("stage62a/before");
	private static final PRenderQueue.RenderStage[] SOURCES = {PRenderQueue.RenderStage.ENTITIES,
			PRenderQueue.RenderStage.SOLID_BLOCKS, PRenderQueue.RenderStage.TRANSLUCENT_BLOCKS};
	private static final float[] DEPTH = new float[4];
	private static final List<Object> SAMPLES = new ArrayList<>();
	private static final int[] CALLS = new int[3];
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable Object oldModel;
	private static int phase;
	private static int passes;
	private static int ticks;
	private static int warmup;
	private static int pipelines;
	private static boolean pending;
	private static boolean completed;

	private WorldStageProbe()
	{
	}

	public static void models(PModelRegistrar registrar)
	{
		registrar.model(DATA).texture("0", WHITE);
	}

	public static void register()
	{
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> submit());
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context ->
		{
			if (phase == 0)
				return;
			check(phase == 3, "native solid/depth hooks precede translucent terrain");
			verifyOpaque("after native translucent terrain");
			phase = 4;
		});
		LevelRenderEvents.END_MAIN.addPhaseOrdering(BEFORE, Event.DEFAULT_PHASE);
		LevelRenderEvents.END_MAIN.register(BEFORE, context ->
		{
			if (phase == 0)
				return;
			check(phase == 4, "native terrain precedes late submission");
			verifyOpaque("before late world flush");
			alpha(SOURCES[1], quad(), 3, 0.5f, 0x8000FF00);
			phase = 5;
		});
		LevelRenderEvents.END_MAIN.register(context -> finish());
		ClientTickEvents.END_CLIENT_TICK.register(WorldStageProbe::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
		{
			check(completed && passes == 2 && Arrays.equals(CALLS, new int[]{2, 2, 2}), "both complete stage sequences before shutdown");
			PLibDatabase.LOGGER.info("STAGE62A SHUTDOWN PASS");
		});
	}

	private static void tick(Minecraft mc)
	{
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-world-stages")), "disposable world-stage client marker");
		if (++ticks > 1800)
			throw new AssertionError("STAGE62A timeout phase=" + phase);
		if (completed || mc.player == null || mc.level == null || mc.getOverlay() != null || DATA.getModel() == null)
			return;
		check(mc.player.isAlive() && mc.player.isCreative(), "isolated owner remains alive and creative");
		mc.options.improvedTransparency().set(false);
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			check(DATA.getModel() != oldModel, "real reload replaces baked model");
			reload = null;
			warmup = 0;
		}
		if (passes == 1 && oldModel == null)
		{
			oldModel = DATA.getModel();
			reload = mc.reloadResourcePacks();
			return;
		}
		if (passes == 2)
		{
			completed = true;
			try
			{
				Files.writeString(mc.gameDirectory.toPath().resolve("world-stages-results.json"),
						new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status", "PASS", "epochs", passes,
								"registeredPipelines", pipelines, "stageCalls", CALLS, "samples", SAMPLES)));
			}
			catch (java.io.IOException exception)
			{
				throw new AssertionError(exception);
			}
			PLibDatabase.LOGGER.info("STAGE62A WORLD PASS: epochs={}, pipelines={}, stageCalls={}, pixelSamples={}, ordinaryPolicy=true, customRenderTypes=0", passes, pipelines, Arrays.toString(CALLS), SAMPLES.size());
			mc.getConnection().getConnection().disconnect(Component.literal("Pulse world stages probe"));
			mc.disconnect(new TitleScreen(), false);
			mc.stop();
			return;
		}
		if (++warmup >= 30 && phase == 0)
			pending = true;
	}

	private static void submit()
	{
		if (!pending || phase != 0)
			return;
		verifyPipelines();
		var alpha = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION);
		check(PRenderTypes.usesOit(alpha) && PRenderTypes.isTransparent(alpha), "ordinary policy uses existing OIT-capable material type");
		PGeometryData quad = quad();
		var solid = PRenderTypes.RenderTypeProvider.trianglesEmissiveSolid(PResourceCache.ATLAS_LOCATION);
		for (int patch = 0; patch < 4; patch++)
		{
			float distance = patch < 2 ? 0.3f : 0.7f;
			int color = patch == 0 ? 0xFFFF0000 : patch == 1 ? 0xFF00FF00 : 0xFF000000;
			PRenderQueue.submit(patch == 0 ? SOURCES[0] : SOURCES[1], solid, quad, header(patch, distance, color));
			Vector3f projected = new PGlRenderHost().captureFrame().projection().transformProject(new Vector3f(0, 0, -distance));
			DEPTH[patch] = (projected.z + 1) * 0.5f;
		}
		alpha(SOURCES[0], quad, 0, 0.4f, 0x800000FF);
		alpha(SOURCES[1], quad, 1, 0.2f, 0x800000FF);
		alpha(SOURCES[2], quad, 2, 0.4f, 0x800000FF);
		alpha(SOURCES[0], quad, 2, 0.6f, 0x80FF0000);
		alpha(SOURCES[1], quad, 2, 0.5f, 0x8000FF00);
		alpha(SOURCES[0], quad, 3, 0.4f, 0x800000FF);
		alpha(SOURCES[2], quad, 3, 0.6f, 0x80FF0000);
		phase = 1;
	}

	private static void alpha(PRenderQueue.RenderStage stage, PGeometryData quad, int patch, float distance, int color)
	{
		PRenderQueue.submit(stage, PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(PResourceCache.ATLAS_LOCATION),
				quad, header(patch, distance, color), PTransparencyMode.ORDINARY);
	}

	/** Called only by the test Mixin after production shared operations return. */
	public static void observed(int stage)
	{
		if (phase == 0)
			return;
		check(phase == (stage == 0 ? 1 : stage == 1 ? 2 : 5), "shared stage routing/order stage=" + stage + " phase=" + phase);
		CALLS[stage]++;
		if (stage < 2)
			verifyOpaque(stage == 0 ? "before native translucent depth copy" : "after native translucent features/depth capture");
		phase = stage == 0 ? 2 : stage == 1 ? 3 : 6;
	}

	private static void verifyPipelines()
	{
		int count = 0;
		for (var field : PRenderTypes.RenderPipelinesProvider.class.getDeclaredFields())
			if (field.getType() == RenderPipeline.class)
				try
				{
					RenderPipeline pipeline = (RenderPipeline)field.get(null);
					check(RenderPipelines.getStaticPipelines().stream().filter(entry -> entry.getLocation().equals(pipeline.getLocation())).toList().equals(List.of(pipeline)), "native unique pipeline registry includes " + field.getName());
					count++;
				}
				catch (IllegalAccessException exception)
				{
					throw new AssertionError(exception);
				}
		check(count > 0 && (pipelines == 0 || pipelines == count), "all existing pipelines survive reload");
		pipelines = count;
	}

	private static void finish()
	{
		if (phase == 0)
			return;
		check(phase == 6, "native late stage callback must execute once before END_MAIN observer");
		float alpha = 128f / 255;
		float inverse = 1 - alpha;
		float[][] expected = {{1, 0, 0}, {0, inverse, alpha},
				{alpha * inverse * inverse, alpha * inverse, alpha}, {alpha * inverse * inverse, alpha * inverse, alpha}};
		for (int patch = 0; patch < 4; patch++)
			verify(patch, expected[patch], "after native translucent particles/world flush");
		PLibDatabase.LOGGER.info("STAGE62A EPOCH PASS: epoch={}, mixedSourceAlpha={}, stageCalls={}", passes, Arrays.toString(pixel(2)), Arrays.toString(CALLS));
		phase = 0;
		pending = false;
		passes++;
		warmup = 0;
	}

	private static void verifyOpaque(String boundary)
	{
		for (int patch = 0; patch < 4; patch++)
			verify(patch, patch == 0 ? new float[]{1, 0, 0} : patch == 1 ? new float[]{0, 1, 0} : new float[]{0, 0, 0}, boundary);
	}

	private static void verify(int patch, float[] expected, String boundary)
	{
		float[] actual = pixel(patch);
		SAMPLES.add(Map.of("epoch", passes, "boundary", boundary, "patch", patch, "expectedRgb", expected.clone(), "actualRgbDepth", actual, "expectedDepth", DEPTH[patch]));
		for (int channel = 0; channel < 3; channel++)
			check(Math.abs(actual[channel] - expected[channel]) < 0.015f,
					boundary + " patch=" + patch + " expected RGB=" + Arrays.toString(expected) + " actual RGB/depth=" + Arrays.toString(actual));
		check(Math.abs(actual[3] - DEPTH[patch]) < 0.00001f,
				boundary + " patch=" + patch + " expected depth=" + DEPTH[patch] + " actual=" + actual[3]);
	}

	private static PInstanceHeader header(int patch, float distance, int color)
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

	private static PGeometryData quad()
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

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE62A: " + message);
	}
}
