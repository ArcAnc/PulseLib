/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Owns one fresh native-world run; does not run the earlier content/animation/offscreen suites. */
public final class WorldRegressionRun
{
	private static int phase;
	private static int initialModelEpochs;
	private static boolean complete;
	private static boolean improved;
	private static long deadline;
	private static int width;
	private static int height;
	private static int framebufferWidth;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable CompletableFuture<Void> setup;

	private WorldRegressionRun()
	{
	}

	static void beginExistingWorld(CompletableFuture<Void> preparation)
	{
		initialModelEpochs = ClientTestPlugin.models;
		improved = Minecraft.getInstance().options.improvedTransparency().get();
		setup = preparation;
		phase = -1;
		deadline = System.nanoTime() + 150_000_000_000L;
		System.setProperty("pulselib.test.worldRegression", "true");
	}

	public static boolean enabled()
	{
		return Boolean.getBoolean("pulselib.test.worldRegression");
	}

	public static boolean h13Active()
	{
		return enabled() && phase == 1;
	}

	public static boolean h14Active()
	{
		return enabled() && phase >= 2 && phase <= 4;
	}

	public static void tick()
	{
		Minecraft mc = Minecraft.getInstance();
		if (complete)
			return;
		if (deadline == 0)
			deadline = System.nanoTime() + 150_000_000_000L;
		DemoChecks.check(System.nanoTime() < deadline, "Native world regression deadline phase=" + phase);
		if (mc.getOverlay() != null || ClientTestPlugin.GLTF.getModel() == null)
			return;
		if (phase == 0)
		{
			initialModelEpochs = ClientTestPlugin.models;
			DemoChecks.plugins(true);
			improved = mc.options.improvedTransparency().get();
			GLFW.glfwHideWindow(mc.getWindow().handle());
			DisposableDemoWorlds.create();
			phase = -1;
		}
		if (phase == -1 && mc.level != null && mc.player != null)
		{
			requireWorld();
			if (setup == null)
			{
				var server = mc.getSingleplayerServer();
				setup = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayers().getFirst();
					player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
					player.teleportTo(player.level(), 10.5, 140, 10.5, java.util.Set.of(), 0, 0, true);
				}, server);
			}
			if (setup.isDone() && mc.screen == null && mc.player.tickCount >= 30 &&
					mc.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR &&
					(!CanonicalProfile.smokeWorld() || mc.level.hasChunk(DemoProbe.sceneBlockPosition().getX() >> 4, DemoProbe.sceneBlockPosition().getZ() >> 4)))
			{
				setup.join();
				PLibDatabase.LOGGER.info("NATIVE WORLD READY: screen=null, spectator, ticks={}", mc.player.tickCount);
				phase = 1;
			}
		}
	}

	public static void lockCamera()
	{
		if (h13Active())
			WorldRenderRegressionHarness.lockCamera();
		if (h14Active())
			H14WorldHarness.lockWorldCamera();
	}

	public static void frame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (complete || phase <= 0 || mc.level == null || mc.player == null || mc.getOverlay() != null)
			return;
		requireWorld();
		try
		{
			if (phase == 1)
			{
				if (WorldRenderRegressionHarness.frame())
					phase = 2;
				return;
			}
			if (phase == 3)
			{
				if (!reload.isDone())
					return;
				reload.join();
				H14WorldHarness.reloaded();
				phase = 4;
			}
			if (!H14WorldHarness.frame())
				return;
			if (phase == 2)
			{
				var windowWidth = BufferUtils.createIntBuffer(1);
				var windowHeight = BufferUtils.createIntBuffer(1);
				GLFW.glfwGetWindowSize(mc.getWindow().handle(), windowWidth, windowHeight);
				width = windowWidth.get(0);
				height = windowHeight.get(0);
				framebufferWidth = mc.getMainRenderTarget().width;
				GLFW.glfwSetWindowSize(mc.getWindow().handle(), width + 32, height + 16);
				reload = mc.reloadResourcePacks();
				phase = 3;
				return;
			}
			DemoChecks.check(mc.getMainRenderTarget().width != framebufferWidth, "Actual native world framebuffer must resize");
			DemoChecks.check(WorldRenderRegressionHarness.cases() == 12 && H14WorldHarness.cases() == 15, "All native H13/H14 cases must finish");
			DemoChecks.check(ClientTestPlugin.models == initialModelEpochs + 1 && H14WorldHarness.hooks() > 0, "World reload and actual Fabulous hook complete");
			mc.options.improvedTransparency().set(improved);
			GLFW.glfwSetWindowSize(mc.getWindow().handle(), width, height);
			complete = true;
			DemoChecks.writeResult(Map.of("status", "PASS", "fixture", "native-world", "h13Cases", WorldRenderRegressionHarness.cases(), "h14Cases", H14WorldHarness.cases(),
					"fabulousHooks", H14WorldHarness.hooks(), "modelEpochs", ClientTestPlugin.models, "resized", true, "origin", DemoChecks.jar(WorldRegressionRun.class)));
			PLibDatabase.LOGGER.info("NATIVE WORLD PASS: H13=12 H14=15 actual terrain/rain/reload/resize");
			if (CanonicalProfile.smokeWorld() || CanonicalProfile.oitWorld())
				CanonicalRun.worldComplete();
			else
				mc.stop();
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot save native H14 evidence", exception);
		}
	}

	public static void stopped()
	{
		DemoChecks.check(complete, "Native world suites complete before shutdown");
		PLibDatabase.LOGGER.info("NATIVE WORLD SHUTDOWN PASS");
	}

	static void requireWorld()
	{
		DemoChecks.world(Minecraft.getInstance().getSingleplayerServer(), true);
	}

	static void weather(net.minecraft.server.level.ServerLevel level, boolean raining)
	{
		var weather = level.getWeatherData();
		weather.setClearWeatherTime(raining ? 0 : 6000);
		weather.setRainTime(raining ? 6000 : 0);
		weather.setRaining(raining);
		weather.setThundering(false);
	}

	static Matrix4f viewProjection()
	{
		var frame = new PGlRenderHost().captureFrame();
		return new Matrix4f(frame.projection()).mul(frame.modelView());
	}

	static float[] pixel(RenderTarget target, int x, int y)
	{
		try (var binding = new H14GpuTargets.Binding(target, false))
		{
			FloatBuffer pixel = BufferUtils.createFloatBuffer(4);
			float[] result = new float[5];
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, pixel);
			pixel.get(result, 0, 4);
			pixel.clear();
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, pixel);
			result[4] = pixel.get(0);
			return result;
		}
	}
}
