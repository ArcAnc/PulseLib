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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Automation creates a fresh save in the marked, isolated Gradle run directory.
 * Ordinary manual runClient/demo launches never enter this code.
 */
public final class DisposableTestWorlds
{
	private static final String WORLD = "pulselib-smoke";
	private static final String MARKER = "PulseLib disposable test world\n";
	private static boolean started;
	private static boolean profilingWindowHidden;

	private DisposableTestWorlds()
	{
	}

	public static void prepareAutomationWindow()
	{
		if (System.getProperty("pulselib.test.runDirectory") != null)
		{
			Minecraft.getInstance().options.pauseOnLostFocus = false;
		}
		if (Boolean.getBoolean("pulselib.test.oitPerf") && !profilingWindowHidden)
		{
			Minecraft mc = Minecraft.getInstance();
			// Hiding preserves the GL context/framebuffer, but prevents UI input
			// from loading another scene during a controlled measurement series.
			GLFW.glfwHideWindow(mc.getWindow().getWindow());
			profilingWindowHidden = true;
		}
	}

	public static void loadIfRequested()
	{
		Minecraft mc = Minecraft.getInstance();
		if (!Boolean.getBoolean("pulselib.test.world") || started || mc.getOverlay() != null || mc.level != null)
		{
			return;
		}
		Path world = runDirectory().resolve("saves").resolve(WORLD);
		try
		{
			// Never adopt, load, delete or overwrite a pre-existing save, even if named like a test.
			if (Files.exists(world) || Files.isSymbolicLink(world.getParent()))
			{
				throw new IllegalStateException("Refusing pre-existing test save: " + world);
			}
			Files.createDirectories(world);
			Files.writeString(world.resolve(".pulselib-disposable"), MARKER, StandardOpenOption.CREATE_NEW);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot create disposable test save", exception);
		}
		started = true;
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		mc.createWorldOpenFlows().createFreshLevel(WORLD,
				new LevelSettings("PulseLib disposable smoke", GameType.CREATIVE, false, Difficulty.PEACEFUL,
						true, rules, WorldDataConfiguration.DEFAULT),
				new WorldOptions(0x50554C5345L, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
	}

	public static void requireCurrentWorld()
	{
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSingleplayerServer() == null)
		{
			throw new IllegalStateException("Automated scene needs its disposable integrated server");
		}
		Path expected = runDirectory().resolve("saves").resolve(WORLD);
		Path actual = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
		try
		{
			if (!actual.equals(expected) || Files.isSymbolicLink(actual) ||
					!Files.readString(actual.resolve(".pulselib-disposable")).equals(MARKER))
			{
				throw new IllegalStateException("Refusing unmarked or unexpected world: " + actual);
			}
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Disposable world marker is missing", exception);
		}
	}

	private static Path runDirectory()
	{
		String configured = System.getProperty("pulselib.test.runDirectory");
		if (configured == null)
		{
			throw new IllegalStateException("Automation requires an isolated Gradle test run directory");
		}
		Path expected = Path.of(configured).toAbsolutePath().normalize();
		Path actual = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
		try
		{
			if (!actual.equals(expected) || Files.isSymbolicLink(actual) ||
					!Files.readString(actual.resolve(".pulselib-test-run")).equals("PulseLib disposable test run\n"))
			{
				throw new IllegalStateException("Refusing unmarked or unexpected run directory: " + actual);
			}
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Test run marker is missing", exception);
		}
		return actual;
	}
}
