/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61worldserver;

import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Native glass/water and a lit wall in the strictly isolated transparency world. */
public final class TransparencyScene
{
	private TransparencyScene()
	{
	}

	public static void prepare(MinecraftServer server)
	{
		if (!server.getWorldData().getLevelName().equals("fabric-world-transparency"))
			throw new IllegalStateException("STAGE62B requires its own disposable world");
		for (int x = -1; x <= 0; x++)
			for (int z = -1; z <= 1; z++)
				server.overworld().getChunk(x, z);
		for (String command : new String[]{
				"gamerule minecraft:advance_time false", "time set noon", "weather clear",
				"fill -12 79 -8 12 79 17 minecraft:stone",
				"fill -12 80 16 12 86 16 minecraft:sea_lantern",
				"fill -9 79 11 -1 85 11 minecraft:blue_stained_glass",
				"fill 0 79 10 8 85 12 minecraft:glass",
				"fill 1 80 11 7 84 11 minecraft:water"})
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
		if (!server.overworld().getBlockState(new BlockPos(-5, 82, 11)).is(Blocks.BLUE_STAINED_GLASS) ||
				!server.overworld().getBlockState(new BlockPos(2, 82, 11)).is(Blocks.WATER) ||
				!server.overworld().getBlockState(new BlockPos(0, 82, 16)).is(Blocks.SEA_LANTERN))
			throw new AssertionError("STAGE62B native terrain must exist before the client joins");
		PLibDatabase.LOGGER.info("STAGE62B SCENE PASS: isolated native glass/water, fixed noon, no mobs");
	}
}
