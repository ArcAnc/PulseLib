/**
 * @author ArcAnc
 * Created at: 10.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Standalone fresh-world prerequisites; the canonical suite runs after native camera/render work. */
public final class PlayerMotionRun
{
	private static int phase;
	private static long deadline;
	private static @Nullable CompletableFuture<Void> setup;

	private PlayerMotionRun()
	{
	}

	public static void tick()
	{
		Minecraft mc = Minecraft.getInstance();
		if (deadline == 0)
			deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(8);
		DemoChecks.check(System.nanoTime() < deadline, "Player motion deadline phase=" + phase);
		if (phase == 0)
		{
			if (mc.getOverlay() != null || ClientTestPlugin.GLTF.getModel() == null)
				return;
			DemoChecks.plugins(true);
			GLFW.glfwHideWindow(mc.getWindow().handle());
			DisposableDemoWorlds.create();
			phase = 1;
		}
		if (phase == 1 && mc.level != null && mc.player != null)
		{
			DemoChecks.world(mc.getSingleplayerServer(), true);
			if (setup == null)
			{
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				setup = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					var level = player.level();
					for (int x = 5; x <= 16; x++)
						for (int z = 5; z <= 18; z++)
						{
							level.setBlockAndUpdate(new BlockPos(x, 139, z), Blocks.STONE.defaultBlockState());
							for (int y = 140; y <= 147; y++)
								level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
						}
					player.setGameMode(GameType.CREATIVE);
					player.teleportTo(level, 10.5, 140, 10.5, Set.of(), 0, 0, true);
				}, server);
			}
			if (!setup.isDone() || mc.screen != null || mc.player.tickCount < 30)
				return;
			setup.join();
			phase = 2;
		}
	}

	public static void frame()
	{
		if (PlayerMotionHarness.enabled() && phase == 2)
			PlayerMotionHarness.frame();
	}
}
