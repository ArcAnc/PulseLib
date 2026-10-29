/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.renderer.PEntityRenderer;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Native world player versus real PulseLib owner, with isolated controls and RGB/depth readback. */
public final class Stage52PlayerOcclusionProbe
{
	public static final PModelData MODEL = PModelData.entity(PLibDatabase.rl("stage42b3_owner"), PGeckoModelLoader.INSTANCE.id());
	private static final boolean ARMOR_MATRIX = Boolean.getBoolean("pulselib.stage52b4aProbe");
	private static final UUID PLAYER_ID = new UUID(0, 0x526a01);
	private static final String[] STATES = {"background", "player", "entity-behind", "both-behind", "entity-front", "both-front"};
	private static final List<CaseData> RECORDS = new ArrayList<>();
	private static final List<String> FAILURES = new ArrayList<>();
	private static final int[][] SAMPLES = new int[9][2];
	private static final Matrix4f READBACK_PROJECTION = new Matrix4f();
	private static List<float[]> early = List.of();
	private static List<float[]> afterNativeFeatures = List.of();
	private static List<float[]> beforeResolve = List.of();
	private static List<float[]> nativeItem = List.of();
	private static boolean started;
	private static boolean completed;
	private static boolean advance;
	private static boolean armed;
	private static boolean originalImproved;
	private static boolean originalHideGui;
	private static CameraType originalCamera = CameraType.FIRST_PERSON;
	private static GraphicsPreset originalPreset = GraphicsPreset.CUSTOM;
	private static @Nullable CompletableFuture<Void> reload;
	private static int phase;
	private static int step;
	private static int startTick;
	private static int frames;
	private static int comparisons;
	private static int submissions;
	private static long deadline;

	private Stage52PlayerOcclusionProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52PlayerOcclusionProbe::opaque);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52PlayerOcclusionProbe::nativeFeatures);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52PlayerOcclusionProbe::nativeLayers);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52PlayerOcclusionProbe::finish);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52PlayerOcclusionProbe::shutdown);
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.getOverlay() != null || MODEL.getModel() == null)
			return;
		if (!started)
		{
			check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-player-occlusion")), "Missing disposable client marker");
			originalImproved = mc.options.improvedTransparency().get();
			originalHideGui = mc.options.hideGui;
			originalCamera = mc.options.getCameraType();
			originalPreset = mc.options.graphicsPreset().get();
			mc.options.hideGui = true;
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			var player = new RemotePlayer(mc.level, new GameProfile(PLAYER_ID, "OcclusionPlayer"));
			player.setId(0x526a01);
			mc.level.addEntity(player);
			started = true;
			deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(ARMOR_MATRIX ? 480 : 180);
			mode();
		}
		check(System.nanoTime() < deadline, "Occlusion matrix timed out: phase=" + phase + " step=" + step);
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			reload = null;
			mode();
		}
		if (advance)
		{
			advance = false;
			step++;
			if (step == (ARMOR_MATRIX ? 48 : 12))
			{
				step = 0;
				phase++;
				if (phase == (ARMOR_MATRIX ? 6 : 4))
				{
					completed = true;
					if (ARMOR_MATRIX)
						Stage52ArmorOcclusionProbe.complete();
					writeReport();
					mc.options.improvedTransparency().set(originalImproved);
					mc.options.graphicsPreset().set(originalPreset);
					mc.options.setCameraType(originalCamera);
					mc.options.hideGui = originalHideGui;
					if (Boolean.getBoolean("pulselib.stage2Probe"))
						Stage2PluginProbe.verify(true);
					PLibDatabase.LOGGER.info("PLAYER OCCLUSION {}: states={}, comparisons={}, mismatches={}, submissions={}; controls, opaque/translucent, both directions, native modes before/after reload", FAILURES.isEmpty() ? "PASS" : "FAIL", RECORDS.size(), comparisons, FAILURES.size(), submissions);
					mc.stop();
					return;
				}
				if (phase == (ARMOR_MATRIX ? 3 : 2))
				{
					reload = mc.reloadResourcePacks();
					return;
				}
				mode();
			}
			startTick = mc.player.tickCount;
			frames = 0;
		}
		KeyMapping.releaseAll();
		place(mc.player, 0, -6);
		mc.player.setYRot(0);
		mc.player.yRotO = 0;
		mc.player.setXRot(0);
		mc.player.xRotO = 0;
		var player = (RemotePlayer)mc.level.getEntity(0x526a01);
		check(player != null, "Native remote player is present");
		if (ARMOR_MATRIX)
			Stage52ArmorOcclusionProbe.equip(player, step / 12, phase);
		boolean playerCentered = state() == 1 || state() == 3 || state() == 5;
		place(player, playerCentered ? 0 : -3, 0);
		player.setYRot(180);
		player.yRotO = player.yBodyRot = player.yBodyRotO = player.yHeadRot = player.yHeadRotO = 180;
		for (var entity : mc.level.entitiesForRendering())
			if (entity instanceof TestEntity)
				place(entity, state() >= 2 ? 0 : 3, state() >= 4 ? -2 : 2);
	}

	private static void mode()
	{
		var mc = Minecraft.getInstance();
		if (ARMOR_MATRIX)
			mc.options.graphicsPreset().set(GraphicsPreset.values()[phase % 3]);
		else
			mc.options.improvedTransparency().set(phase % 2 == 1);
		mc.levelRenderer.allChanged();
		frames = 0;
		startTick = mc.player.tickCount + 40;
		armed = false;
	}

	private static int material()
	{
		return step % 12 / 6;
	}

	private static int state()
	{
		return step % 6;
	}

	private static void place(Entity entity, double x, double z)
	{
		entity.setPos(x, 80, z);
		entity.xo = entity.xOld = x;
		entity.yo = entity.yOld = 80;
		entity.zo = entity.zOld = z;
		entity.setDeltaMovement(0, 0, 0);
	}

	private static void opaque(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		var mc = Minecraft.getInstance();
		if (!started || completed || advance || reload != null || mc.getOverlay() != null || mc.player.tickCount < startTick + 5)
			return;
		if (!mc.levelRenderer.isSectionCompiledAndVisible(new BlockPos(0, 81, 6)) || ++frames < 8)
			return;
		check(Minecraft.useShaderTransparency() == (ARMOR_MATRIX ? phase % 3 == 2 : phase % 2 == 1), "Native mode differs from requested mode");
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		var frame = new PGlRenderHost().captureFrame();
		READBACK_PROJECTION.set(frame.projection());
		var vp = new Matrix4f(frame.projection()).mul(frame.modelView());
		RenderTarget target = mc.getMainRenderTarget();
		List<float[]> values = new ArrayList<>();
		for (int sample = 0; sample < 9; sample++)
		{
			var screen = vp.transform(new Vector4f((float)((sample % 3 - 1) * 0.08 - camera.x),
					(float)(81.05 + (sample / 3 - 1) * 0.12 - camera.y), (float)-camera.z, 1));
			int x = (int)((screen.x / screen.w + 1) * 0.5f * target.width);
			int y = (int)((screen.y / screen.w + 1) * 0.5f * target.height);
			check(x > 0 && y > 0 && x < target.width && y < target.height, "Torso sample is inside viewport");
			SAMPLES[sample][0] = x;
			SAMPLES[sample][1] = y;
			values.add(pixel(target, x, y));
		}
		early = values;
		armed = true;
	}

	private static void nativeFeatures(RenderLevelStageEvent.AfterTranslucentFeatures event)
	{
		if (armed && !advance)
			afterNativeFeatures = pixels(Minecraft.getInstance().getMainRenderTarget());
	}

	private static void nativeLayers(RenderLevelStageEvent.AfterWeather event)
	{
		if (!armed || advance)
			return;
		var mc = Minecraft.getInstance();
		var item = mc.levelRenderer.getItemEntityTarget();
		check(item != null, "Visible PulseLib owner retains native item/entity target");
		beforeResolve = pixels(mc.getMainRenderTarget());
		nativeItem = pixels(item);
	}

	private static void finish(RenderLevelStageEvent.AfterLevel event)
	{
		if (!armed || advance)
			return;
		var mc = Minecraft.getInstance();
		List<float[]> result = pixels(mc.getMainRenderTarget());
		List<Pixel> pixels = new ArrayList<>();
		for (int sample = 0; sample < 9; sample++)
			pixels.add(new Pixel(SAMPLES[sample][0], SAMPLES[sample][1], early.get(sample), afterNativeFeatures.get(sample), beforeResolve.get(sample), nativeItem.get(sample), result.get(sample),
					distanceFromCamera(Math.min(result.get(sample)[4], nativeItem.get(sample)[4]))));
		RECORDS.add(new CaseData(phase, READBACK_PROJECTION.get(new float[16]), mc.options.graphicsPreset().get().toString(), ARMOR_MATRIX ? Stage52ArmorOcclusionProbe.variant() : "skin", Minecraft.useShaderTransparency(), material() == 0 ? "opaque" : "translucent", STATES[state()], pixels));
		writeReport();
		if (state() == 5)
			compare();
		if ((!ARMOR_MATRIX && phase < 2 || ARMOR_MATRIX && phase == 2) && state() != 0)
			Screenshot.grab(mc.gameDirectory, "player-occlusion-" + phase + "-" + (ARMOR_MATRIX ? Stage52ArmorOcclusionProbe.variant() + "-" : "") + material() + "-" + STATES[state()] + ".png", mc.getMainRenderTarget(), 1,
					message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		armed = false;
		advance = true;
	}

	private static void compare()
	{
		int base = RECORDS.size() - 6;
		if (ARMOR_MATRIX)
			Stage52ArmorOcclusionProbe.verifyControl(RECORDS.get(base + 1).pixels.stream().map(Pixel::nearestDistance).toList(), phase, material());
		for (int sample = 0; sample < 9; sample++)
		{
			Pixel background = RECORDS.get(base).pixels.get(sample);
			Pixel player = RECORDS.get(base + 1).pixels.get(sample);
			Pixel entityBehind = RECORDS.get(base + 2).pixels.get(sample);
			Pixel bothBehind = RECORDS.get(base + 3).pixels.get(sample);
			Pixel entityFront = RECORDS.get(base + 4).pixels.get(sample);
			Pixel bothFront = RECORDS.get(base + 5).pixels.get(sample);
			check(distance(background.finalFrame, player.finalFrame) > 0.05f, "Player-only control actually covers torso sample " + sample);
			check(distance(background.finalFrame, entityBehind.finalFrame) > 0.05f && distance(background.finalFrame, entityFront.finalFrame) > 0.05f, "Entity-only controls emit visible geometry");
			check(nearestDepth(player) < nearestDepth(background) - 0.0001f, "Native player control writes nearer depth");
			require(distance(player.finalFrame, bothBehind.finalFrame) < 0.025f, "entity behind player changes RGB", sample, player.finalFrame, bothBehind.finalFrame);
			require(Math.abs(player.nearestDistance - bothBehind.nearestDistance) < 0.0005f, "entity behind player changes nearest depth", sample, player.finalFrame, bothBehind.finalFrame);
			float transmission = entityFront.finalFrame[1] / background.finalFrame[1];
			check(background.finalFrame[1] > 0.1f && transmission >= 0 && transmission < 0.9f, "Red entity-only control measures nonzero opacity");
			float[] expected = new float[3];
			for (int channel = 0; channel < 3; channel++)
				expected[channel] = entityFront.finalFrame[channel] + transmission * (player.finalFrame[channel] - background.finalFrame[channel]);
			require(distance(expected, bothFront.finalFrame) < 0.035f, "entity in front has incorrect ordered-alpha RGB", sample, expected, bothFront.finalFrame);
			if (material() == 0)
				require(Math.abs(entityFront.nearestDistance - bothFront.nearestDistance) < 0.0005f, "opaque entity in front loses its depth", sample, entityFront.finalFrame, bothFront.finalFrame);
		}
		PLibDatabase.LOGGER.info("PLAYER OCCLUSION MATRIX: phase={}, improved={}, material={}, states={}, comparisons={}, mismatches={}", phase, Minecraft.useShaderTransparency(), material() == 0 ? "opaque" : "translucent", RECORDS.size(), comparisons, FAILURES.size());
	}

	/** Recover view-space distance using this frame's actual projection, including preset far-plane transitions. */
	private static float distanceFromCamera(float depth)
	{
		var view = new Matrix4f(READBACK_PROJECTION).invert().transform(new Vector4f(0, 0, depth * 2 - 1, 1));
		return -view.z / view.w;
	}

	private static float nearestDepth(Pixel pixel)
	{
		return Math.min(pixel.finalFrame[4], pixel.nativeItem[4]);
	}

	private static float distance(float[] first, float[] second)
	{
		float maximum = 0;
		for (int channel = 0; channel < 3; channel++)
			maximum = Math.max(maximum, Math.abs(first[channel] - second[channel]));
		return maximum;
	}

	private static void require(boolean condition, String message, int sample, float[] expected, float[] actual)
	{
		comparisons++;
		if (!condition)
			FAILURES.add("phase=" + phase + " material=" + material() + " sample=" + sample + " " + message + " expected=" + java.util.Arrays.toString(expected) + " actual=" + java.util.Arrays.toString(actual));
	}

	private static List<float[]> pixels(RenderTarget target)
	{
		List<float[]> result = new ArrayList<>();
		for (int[] sample : SAMPLES)
			result.add(pixel(target, sample[0], sample[1]));
		return result;
	}

	private static float[] pixel(RenderTarget target, int x, int y)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
			check(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Complete player occlusion readback framebuffer");
			var rgba = BufferUtils.createFloatBuffer(4);
			var depth = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, rgba);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
			return new float[] {rgba.get(0), rgba.get(1), rgba.get(2), rgba.get(3), depth.get(0)};
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void writeReport()
	{
		try
		{
			var report = new Report(!completed ? "INCOMPLETE" : FAILURES.isEmpty() ? "PASS" : "FAIL", comparisons, submissions, RECORDS, FAILURES);
			Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("player-occlusion-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
		}
		catch (IOException exception)
		{
			throw new UncheckedIOException(exception);
		}
	}

	private static void shutdown(GameShuttingDownEvent event)
	{
		check(completed && RECORDS.size() == (ARMOR_MATRIX ? 288 : 48) && comparisons == (ARMOR_MATRIX ? 1512 : 252) && submissions > 0, "Entire occlusion matrix and real submissions must finish");
		check(FAILURES.isEmpty(), "Player occlusion regression failed: " + FAILURES.size() + " mismatches; see player-occlusion-results.json");
		PLibDatabase.LOGGER.info("PLAYER OCCLUSION SHUTDOWN PASS");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private record Pixel(int x, int y, float[] afterOpaque, float[] afterNativeFeatures, float[] beforeResolve, float[] nativeItem, float[] finalFrame, float nearestDistance)
	{
	}

	private record CaseData(int phase, float[] projection, String preset, String owner, boolean improved, String material, String state, List<Pixel> pixels)
	{
	}

	private record Report(String status, int comparisons, int submissions, List<CaseData> cases, List<String> failures)
	{
	}

	public static final class EntityRenderer extends PEntityRenderer<TestEntity, PEntityRenderState.LivingImpl<TestEntity>>
	{
		public EntityRenderer(EntityRendererProvider.Context context)
		{
			super(context, MODEL, PRenderTypes.RenderTypeProvider::trianglesSolid);
		}

		@Override
		public PEntityRenderState.LivingImpl<TestEntity> createRenderState()
		{
			return PLibHelper.livingRenderState();
		}

		@Override
		protected PMeshRenderContext resolveMeshRender(TestEntity owner, PBakedBone bone, PBakedMesh mesh, PMeshRenderContext inherited, float partialTick)
		{
			submissions++;
			return inherited.withColor(material() == 0 ? 0xFFFF0000 : 0x80FF0000).withEmissive(true).
					withAlphaMode(material() == 0 ? PAlphaMode.OPAQUE : PAlphaMode.TRANSLUCENT);
		}
	}
}
