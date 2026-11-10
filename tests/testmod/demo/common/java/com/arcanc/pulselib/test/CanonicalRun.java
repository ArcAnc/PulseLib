/**
 * @author ArcAnc
 * Created at: 11.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.entity.renderer.PTestArmor;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import com.arcanc.pulselib.content.registration.player.PPlayerAcrobaticDemo;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Joins migrated drivers at their completion boundaries in one disposable client process. */
public final class CanonicalRun
{
	private static boolean started;
	private static boolean renderProbe;
	private static boolean complete;
	private static int offscreenCases;

	private CanonicalRun()
	{
	}

	public static boolean noWorldSmoke()
	{
		return CanonicalProfile.route("smoke") && !CanonicalProfile.world();
	}

	public static boolean baselineOnly()
	{
		return CanonicalProfile.route("baseline") && !CanonicalProfile.world();
	}

	public static void manualTick()
	{
		Minecraft mc = Minecraft.getInstance();
		if (!started && CanonicalProfile.world() && mc.getOverlay() == null && ClientTestPlugin.GLTF.getModel() != null)
		{
			DemoChecks.plugins(true);
			DisposableDemoWorlds.create();
			started = true;
		}
	}

	public static void tick()
	{
		Minecraft mc = Minecraft.getInstance();
		if (started || mc.getOverlay() != null || ClientTestPlugin.GLTF.getModel() == null)
			return;
		DemoChecks.plugins(true);
		GLFW.glfwHideWindow(mc.getWindow().handle());
		if (noWorldSmoke())
		{
			DemoProbe.models(mc);
			SmokeHarness.begin();
		}
		started = true;
	}

	public static void frame()
	{
		if (!started || complete || Minecraft.getInstance().getOverlay() != null)
			return;
		if (baselineOnly())
		{
			baseline();
			finishOit();
		}
		else if (noWorldSmoke() && SmokeHarness.completed())
		{
			renderCases();
			SmokeLifecycleHarness.explicitOnly();
			complete = true;
			DemoChecks.writeResult(Map.of("status", "PASS", "side", "client", "fixture", "canonical-smoke-menu",
					"origin", DemoChecks.jar(CanonicalRun.class), "renderCases", RenderRegressionHarness.cases(),
					"modelEpochs", ClientTestPlugin.models, "format", format(), "smoke", SmokeHarness.result()));
			PLibDatabase.LOGGER.info("PULSE TEST CLIENT PASS");
			Minecraft.getInstance().stop();
		}
	}

	public static String format()
	{
		DemoChecks.check(TestEntityRender.MODEL_DATA.getModelLoaderId().equals(PTestArmor.MODEL_DATA.getModelLoaderId()), "Entity and armor use the same selected parser");
		return TestEntityRender.MODEL_DATA.getModelLoaderId().toString();
	}

	public static boolean renderProbeActive()
	{
		return renderProbe;
	}

	private static void renderCases()
	{
		renderProbe = true;
		try
		{
			RenderRegressionHarness.run();
		}
		finally
		{
			renderProbe = false;
		}
	}

	public static void beginWorldSuites()
	{
		Minecraft mc = Minecraft.getInstance();
		renderCases();
		PPlayerAnimations.getHandle(mc.player, PPlayerBallDemo.ID).stopAll();
		PPlayerAnimations.getHandle(mc.player, PPlayerAcrobaticDemo.ID).stopAll();
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		var setup = CompletableFuture.runAsync(() ->
		{
			var player = server.getPlayerList().getPlayer(uuid);
			player.getInventory().clearContent();
			player.setGameMode(GameType.SPECTATOR);
			player.level().removeBlock(DemoProbe.sceneBlockPosition(), false);
			for (var entity : player.level().getAllEntities())
				if (entity instanceof TestEntity)
					entity.discard();
			player.teleportTo(player.level(), 10.5, 140, 10.5, java.util.Set.of(), 0, 0, true);
			// The preceding unload oracle dropped this client chunk through the actual cache path.
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
					player.level().getChunkAt(DemoProbe.sceneBlockPosition()), player.level().getLightEngine(), null, null));
		}, server);
		WorldRegressionRun.beginExistingWorld(setup);
	}

	public static void offscreenComplete()
	{
		offscreenCases = 38;
		System.setProperty("pulselib.test.h14Offscreen", "false");
		System.setProperty("pulselib.test.worldRegression", "true");
	}

	public static void worldComplete()
	{
		System.setProperty("pulselib.test.worldRegression", "false");
		if (CanonicalProfile.smokeWorld())
			DemoProbe.regressionsComplete();
		else
		{
			if (CanonicalProfile.route("baseline"))
				baseline();
			finishOit();
		}
	}

	private static void baseline()
	{
		try
		{
			H14OffscreenHarness.baseline();
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Canonical H14 baseline failed", exception);
		}
	}

	private static void finishOit()
	{
		complete = true;
		DemoChecks.writeResult(Map.of("status", "PASS", "side", "client", "fixture", "canonical-oit",
				"origin", DemoChecks.jar(CanonicalRun.class), "offscreenCases", offscreenCases,
				"world", CanonicalProfile.world(), "baseline", CanonicalProfile.route("baseline"),
				"h13Cases", WorldRenderRegressionHarness.cases(), "h14Cases", H14WorldHarness.cases(), "modelEpochs", ClientTestPlugin.models));
		PLibDatabase.LOGGER.info("CANONICAL OIT PASS");
		Minecraft.getInstance().stop();
	}

	public static void stopped()
	{
		DemoChecks.check(complete, "Canonical composed run completes before shutdown");
		PLibDatabase.LOGGER.info("CANONICAL SHUTDOWN PASS");
	}
}
