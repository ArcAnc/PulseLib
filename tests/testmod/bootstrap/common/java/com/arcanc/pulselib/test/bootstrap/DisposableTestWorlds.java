/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Creates one fresh integrated save inside the marked Gradle run directory; never adopts old saves. */
public final class DisposableTestWorlds
{
	private DisposableTestWorlds()
	{
	}

	public static void create()
	{
		Minecraft mc = Minecraft.getInstance();
		Path expected = BootstrapChecks.runDirectory();
		BootstrapChecks.check(mc.gameDirectory.toPath().toAbsolutePath().normalize().equals(expected), "Client working directory equals generated run root");
		Path saves = expected.resolve("saves"), world = saves.resolve(BootstrapChecks.WORLD);
		try
		{
			BootstrapChecks.check(!Files.exists(world) && !Files.isSymbolicLink(saves), "Refuse every pre-existing save");
			Files.createDirectories(world);
			Files.writeString(world.resolve(".pulselib-disposable"), BootstrapChecks.WORLD_MARKER, StandardOpenOption.CREATE_NEW);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException(exception);
		}
		mc.options.pauseOnLostFocus = false;
		mc.createWorldOpenFlows().createFreshLevel(BootstrapChecks.WORLD,
				new LevelSettings("PulseLib disposable bootstrap", GameType.CREATIVE,
						new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true, WorldDataConfiguration.DEFAULT),
				new WorldOptions(0x50554C5345L, false, false), WorldPresets::createFlatWorldDimensions, mc.screen);
	}
}
