/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

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

public final class DisposableDemoWorlds
{
	private static boolean created;

	private DisposableDemoWorlds()
	{
	}

	public static void create()
	{
		Minecraft mc = Minecraft.getInstance();
		Path expected = DemoChecks.runDirectory();
		DemoChecks.check(mc.gameDirectory.toPath().toAbsolutePath().normalize().equals(expected), "Client working directory equals generated run root");
		Path saves = expected.resolve("saves"), world = saves.resolve(DemoChecks.WORLD);
		try
		{
			DemoChecks.check(!Files.exists(world) && !Files.isSymbolicLink(saves), "Refuse every pre-existing save");
			Files.createDirectories(world);
			Files.writeString(world.resolve(".pulselib-disposable"), DemoChecks.WORLD_MARKER, StandardOpenOption.CREATE_NEW);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException(exception);
		}
		created = true;
		mc.options.pauseOnLostFocus = false;
		mc.createWorldOpenFlows().createFreshLevel(DemoChecks.WORLD,
				new LevelSettings("PulseLib disposable bootstrap", GameType.CREATIVE,
						new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true, WorldDataConfiguration.DEFAULT),
				new WorldOptions(0x50554C5345L, false, false), WorldPresets::createFlatWorldDimensions, mc.screen);
	}
	/** Opens only the save created by this JVM inside its unique marked run root. */
	public static void reopen()
	{
		Path saves = DemoChecks.runDirectory().resolve("saves"), world = saves.resolve(DemoChecks.WORLD);
		try
		{
			DemoChecks.check(created && !Files.isSymbolicLink(saves) && !Files.isSymbolicLink(world) &&
					!Files.isSymbolicLink(world.resolve(".pulselib-disposable")) &&
					Files.readString(world.resolve(".pulselib-disposable")).equals(DemoChecks.WORLD_MARKER) &&
					Files.isRegularFile(world.resolve("level.dat")), "Refuse reentry into any world not created by this run");
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Disposable reentry marker is missing", exception);
		}
		Minecraft.getInstance().createWorldOpenFlows().openWorld(DemoChecks.WORLD, () ->
		{
			throw new IllegalStateException("Cannot reopen the disposable test world");
		});
	}
}
