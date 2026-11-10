/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Owns the canonical render regression and baseline run in one fresh marked world. */
public final class RenderRegressionRun
{
	private static int phase;
	private static boolean complete;
	private static boolean improved;
	private static long deadline;
	private static @Nullable CompletableFuture<Void> setup;

	private RenderRegressionRun()
	{
	}

	public static boolean enabled()
	{
		return Boolean.getBoolean("pulselib.test.renderRegression");
	}

	public static void tick()
	{
		Minecraft mc = Minecraft.getInstance();
		if (complete)
			return;
		if (deadline == 0)
			deadline = System.nanoTime() + 90_000_000_000L;
		DemoChecks.check(System.nanoTime() < deadline, "Render regression deadline phase=" + phase);
		if (mc.getOverlay() != null || ClientTestPlugin.GLTF.getModel() == null)
			return;
		if (phase == 0)
		{
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
					mc.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR)
			{
				setup.join();
				PLibDatabase.LOGGER.info("RENDER READY: screen=null, spectator, ticks={}", mc.player.tickCount);
				phase = 1;
			}
		}
	}

	public static void frame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (complete || phase != 1 || mc.getOverlay() != null)
			return;
		requireWorld();
		RenderRegressionHarness.run();
		try
		{
			H14OffscreenHarness.baseline();
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot capture legacy routing baseline", exception);
		}
		DemoChecks.check(RenderRegressionHarness.cases() == 41, "All canonical RenderRegression cases complete");
		mc.options.improvedTransparency().set(improved);
		complete = true;
		DemoChecks.writeResult(Map.of("status", "PASS", "fixture", "render-regression", "cases", RenderRegressionHarness.cases(), "baseline", true,
				"origin", DemoChecks.jar(RenderRegressionRun.class)));
		PLibDatabase.LOGGER.info("RENDER REGRESSION PASS: 41 canonical cases, native entity/GUI, H14 legacy routing baseline");
		mc.stop();
	}

	public static void stopped()
	{
		DemoChecks.check(complete, "Render regression suites complete before shutdown");
		PLibDatabase.LOGGER.info("RENDER SHUTDOWN PASS");
	}

	static void requireWorld()
	{
		DemoChecks.world(Minecraft.getInstance().getSingleplayerServer(), true);
	}

}
