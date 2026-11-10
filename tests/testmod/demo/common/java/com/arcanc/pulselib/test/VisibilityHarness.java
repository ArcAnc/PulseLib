/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.renderer.base.PBlockRenderState;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Native frustum and GPU visibility for the canonical camera-away reproduction. No geometry substitution. */
public final class VisibilityHarness
{
	private static final BlockPos TEST_BLOCK = new BlockPos(11, 140, 14);
	private static final List<Map<String, Object>> RESULTS = new ArrayList<>();
	private static int phase = -2;
	private static int epoch;
	private static int phaseStart;
	private static int measured;
	private static int query;
	private static int queries;
	private static int callbacks;
	private static long samples;
	private static long deadline;
	private static boolean observing;
	private static boolean frameReady;
	private static boolean frustumVisible;
	private static boolean improved;
	private static boolean complete;
	private static float nativeYaw;
	private static int pulseBlocks;
	private static @Nullable CompletableFuture<Void> setup;
	private static @Nullable CompletableFuture<Void> reload;

	private VisibilityHarness()
	{
	}

	public static boolean enabled()
	{
		return Boolean.getBoolean("pulselib.test.visibilityRegression");
	}

	private static boolean reproduce()
	{
		return Boolean.getBoolean("pulselib.test.reproduceCamera");
	}

	public static void tick()
	{
		Minecraft mc = Minecraft.getInstance();
		if (complete)
			return;
		if (deadline == 0)
			deadline = System.nanoTime() + 120_000_000_000L;
		DemoChecks.check(System.nanoTime() < deadline, "Native visibility deadline phase=" + phase);
		if (mc.getOverlay() != null || ClientTestPlugin.GLTF.getModel() == null)
			return;
		if (phase == -2)
		{
			DemoChecks.plugins(true);
			improved = mc.options.improvedTransparency().get();
			mc.options.improvedTransparency().set(false);
			GLFW.glfwHideWindow(mc.getWindow().handle());
			DisposableDemoWorlds.create();
			phase = -1;
		}
		if (phase == -1 && mc.level != null && mc.player != null)
		{
			DemoChecks.world(mc.getSingleplayerServer(), true);
			if (setup == null)
			{
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				setup = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					DemoChecks.check(player != null, "Server fixture player must exist");
					var level = player.level();
					for (int x = 5; x <= 16; x++)
						for (int z = 5; z <= 18; z++)
						{
							level.setBlockAndUpdate(new BlockPos(x, 139, z), Blocks.STONE.defaultBlockState());
							for (int y = 140; y <= 147; y++)
								level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
						}
					player.getInventory().clearContent();
					player.setGameMode(GameType.CREATIVE);
					player.teleportTo(level, 10.5, 140, 10.5, Set.of(), 0, 0, true);
					level.setBlockAndUpdate(TEST_BLOCK, DemoContent.block.get().defaultBlockState());
					DemoChecks.check(level.getBlockEntity(TEST_BLOCK) instanceof TestBlockEntity, "Server fixture BlockEntity must exist");
				}, server);
			}
			if (!setup.isDone() || mc.screen != null || mc.player.tickCount < 30 || !(mc.level.getBlockEntity(TEST_BLOCK) instanceof TestBlockEntity))
				return;
			setup.join();
			phase = 0;
			phaseStart = mc.player.tickCount;
		}
		if (reload != null && reload.isDone())
		{
			reload.join();
			reload = null;
			epoch = 1;
			mc.options.improvedTransparency().set(true);
			mc.levelRenderer.allChanged();
			phase = 0;
			phaseStart = mc.player.tickCount;
		}
	}

	public static void lockCamera()
	{
		Minecraft mc = Minecraft.getInstance();
		if (!enabled() || complete || phase < 0 || mc.player == null)
			return;
		KeyMapping.releaseAll();
		mc.options.setCameraType(CameraType.FIRST_PERSON);
		mc.player.setPos(10.5, 140, 10.5);
		mc.player.xo = mc.player.xOld = 10.5;
		mc.player.yo = mc.player.yOld = 140;
		mc.player.zo = mc.player.zOld = 10.5;
		float yaw = reproduce() || phase == 1 ? 180.0f : 0.0f;
		mc.player.setYRot(yaw);
		mc.player.yRotO = yaw;
		mc.player.yBodyRot = yaw;
		mc.player.yBodyRotO = yaw;
		mc.player.setXRot(0.0f);
		mc.player.xRotO = 0.0f;
		mc.player.setDeltaMovement(0, 0, 0);
	}

	/** Called at the real LevelRenderer entry; only this world's Pulse owner may contribute query samples. */
	public static void beginWorld(LevelRenderState state)
	{
		observing = false;
		frameReady = false;
		Minecraft mc = Minecraft.getInstance();
		if (!enabled() || complete || phase < 0 || reload != null || mc.getOverlay() != null || mc.player.tickCount - phaseStart < 30)
			return;
		if (phase != 1 && !reproduce() && !mc.levelRenderer.isSectionCompiledAndVisible(TEST_BLOCK))
			return;
		nativeYaw = state.cameraRenderState.yRot;
		float yaw = reproduce() || phase == 1 ? 180.0f : 0.0f;
		if (Math.abs(Mth.wrapDegrees(nativeYaw - yaw)) > 0.001f)
			return;
		DemoChecks.check(mc.options.getCameraType() == CameraType.FIRST_PERSON, "Actual visibility camera must be first person");
		frustumVisible = state.cameraRenderState.cullFrustum.isVisible(new AABB(TEST_BLOCK));
		pulseBlocks = 0;
		for (var block : state.blockEntityRenderStates)
			if (block instanceof PBlockRenderState<?>)
			{
				DemoChecks.check(block.blockPos.equals(TEST_BLOCK), "Only the original fixture block may contribute Pulse world geometry");
				pulseBlocks++;
			}
		for (var entity : state.entityRenderStates)
			DemoChecks.check(!(entity instanceof PEntityRenderState<?>) && (!(entity instanceof PWorldRenderState pulse) || !pulse.pulselib$requiresSeparateTargets()), "No Pulse entity/item/player geometry may contaminate the block query");
		DemoChecks.check(pulseBlocks <= 1, "At most one extracted Pulse block owner");
		samples = 0;
		queries = 0;
		callbacks = 0;
		observing = true;
	}

	/** Scope covers only Pulse mesh flushes, excluding native terrain and fullscreen resolve passes. */
	public static boolean beginSamples()
	{
		if (!observing)
			return false;
		DemoChecks.check(query == 0 && GL15.glGetQueryi(GL15.GL_SAMPLES_PASSED, GL15.GL_CURRENT_QUERY) == 0, "Visibility query must not nest or replace a caller query");
		query = GL15.glGenQueries();
		GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, query);
		return true;
	}

	public static void endSamples(boolean started)
	{
		if (!started)
			return;
		GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
		try
		{
			samples += Integer.toUnsignedLong(GL15.glGetQueryObjectui(query, GL15.GL_QUERY_RESULT));
			queries++;
		}
		finally
		{
			GL15.glDeleteQueries(query);
			query = 0;
		}
	}

	public static void endWorld()
	{
		if (!observing)
			return;
		observing = false;
		DemoChecks.check(query == 0 && queries == 2, "Both actual opaque and transparent Pulse flushes must be queried");
		GlErrors.check();
		DemoChecks.check(GL11.glGetError() == GL11.GL_NO_ERROR, "Native visibility query OpenGL state");
		frameReady = true;
	}

	public static void recordBlock(TestBlockEntity block)
	{
		if (observing && block.getBlockPos().equals(TEST_BLOCK))
			callbacks++;
	}

	public static void frame()
	{
		if (!enabled() || !frameReady || complete)
			return;
		frameReady = false;
		Minecraft mc = Minecraft.getInstance();
		PLibDatabase.LOGGER.info("VISIBILITY SAMPLE epoch={} phase={} yaw={} frustum={} samples={} callbacks={} owners={} queries={}", epoch, phase, nativeYaw, frustumVisible, samples, callbacks, pulseBlocks, queries);
		if (phase != 1)
			requireVisible(samples);
		else
			DemoChecks.check(samples == 0, "A block behind the actual camera must produce zero GPU samples");
		DemoChecks.check(frustumVisible == (phase != 1), "Native block frustum must agree with the requested visibility phase");
		RESULTS.add(Map.of("epoch", epoch, "phase", phase, "yaw", nativeYaw, "frustum", frustumVisible, "samples", samples, "callbacks", callbacks, "owners", pulseBlocks, "queries", queries));
		if (++measured < 4)
			return;
		measured = 0;
		try
		{
			H14OffscreenHarness.save(mc.getMainRenderTarget(), "visibility-" + epoch + "-" + phase);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot save visibility witness", exception);
		}
		phase++;
		phaseStart = mc.player.tickCount;
		if (phase < 3)
			return;
		if (epoch == 0)
		{
			reload = mc.reloadResourcePacks();
			return;
		}
		DemoChecks.check(RESULTS.size() == 24 && ClientTestPlugin.models == 2, "Six visibility phases and real resource reload must complete");
		mc.options.improvedTransparency().set(improved);
		complete = true;
		DemoChecks.writeResult(Map.of("status", "PASS", "fixture", "visibility", "phases", 6, "records", RESULTS, "origin", DemoChecks.jar(VisibilityHarness.class)));
		PLibDatabase.LOGGER.info("VISIBILITY PASS: front-away-front, two native graphics routes, reload, 24 GPU/frustum observations");
		mc.stop();
	}

	/** Canonical Smoke visibility prerequisite, adapted from callback count to actual GPU coverage. */
	private static void requireVisible(long blockSamples)
	{
		check(blockSamples > 0, "Original block entity renderer must run");
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("PulseLib smoke test: " + message);
	}

	public static void stopped()
	{
		DemoChecks.check(complete, "Native visibility suite must complete before shutdown");
		PLibDatabase.LOGGER.info("VISIBILITY SHUTDOWN PASS");
	}
}
