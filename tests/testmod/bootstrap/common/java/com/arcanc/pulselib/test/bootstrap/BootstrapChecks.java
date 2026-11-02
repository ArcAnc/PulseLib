/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap;

import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.plugin.IPulsePluginFinder;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared JAR/lifecycle assertions; contains no Minecraft client type reference. */
public final class BootstrapChecks
{
	public static final String MOD_ID = "pulselibtest_bootstrap";
	public static final String WORLD = "pulselib-bootstrap";
	public static final String RUN_MARKER = "PulseLib disposable test run\n";
	public static final String WORLD_MARKER = "PulseLib disposable test world\n";
	public static int serverPlugins;
	private static boolean serverPassed;

	private BootstrapChecks()
	{
	}

	public static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static String jar(Class<?> type)
	{
		String origin = type.getProtectionDomain().
				getCodeSource().
				getLocation().
				toString();
		check(origin.contains(".jar") && origin.contains("pulselib-testmod-bootstrap"), "External testmod class originates in its packaged JAR: " + origin);
		return origin;
	}

	public static void plugins(boolean client)
	{
		check(PulseServices.PLATFORM.isClient() == client, "Expected physical side");
		check(CommonTestPlugin.constructions == 1 && CommonTestPlugin.registrations == 1 && CommonTestPlugin.events == 1 && CommonTestPlugin.deformers == 1, "Common plugin lifecycle exactly once");
		check(serverPlugins == (client ? 0 : 1), "Physical server plugin selection");
		var candidates = PulseServices.load(IPulsePluginFinder.class).
				findPlugins().stream().
				filter(value -> value.modId().equals(MOD_ID)).
				toList();
		check(candidates.size() == 3 && candidates.stream().
				map(value -> value.side()).
				collect(java.util.stream.Collectors.toSet()).
				equals(Set.of(PulsePlugin.Side.BOTH, PulsePlugin.Side.CLIENT, PulsePlugin.Side.SERVER)),
				"Three packaged plugin candidates and physical-side metadata");
		check(PulsePluginManager.get().pluginIds().containsAll(List.of(id("common"), id(client ? "client" : "server"))) &&
				PulsePluginManager.get().pluginIds().stream().
						filter(value -> value.getNamespace().equals(MOD_ID)).count() == 2,
				"Exactly two side-compatible plugins loaded from external JAR");
		PLibDatabase.LOGGER.info("PULSE BOOTSTRAP PLUGINS PASS: client={}, candidates={}, loaded=2, origin={}", client, candidates.size(), jar(CommonTestPlugin.class));
	}

	public static Path runDirectory()
	{
		String configured = System.getProperty("pulselib.test.runDirectory");
		check(configured != null, "Packaged bootstrap requires its generated disposable run directory");
		Path root = Path.of(configured).
				toAbsolutePath().normalize();
		try
		{
			check(!Files.isSymbolicLink(root) && Files.isDirectory(root) && Files.readString(root.resolve(".pulselib-test-run")).equals(RUN_MARKER), "Generated disposable run marker");
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Missing disposable run marker", exception);
		}
		return root;
	}

	public static void world(MinecraftServer server, boolean integrated)
	{
		Path expected = integrated ? runDirectory().resolve("saves").resolve(WORLD) : runDirectory().resolve(WORLD);
		Path actual = server.getWorldPath(LevelResource.ROOT).
				toAbsolutePath().normalize();
		try
		{
			check(actual.equals(expected) && !Files.isSymbolicLink(actual) && Files.readString(actual.resolve(".pulselib-disposable")).equals(WORLD_MARKER), "Only this run's marked disposable world is used");
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Missing disposable world marker", exception);
		}
	}

	public static void serverStarted(MinecraftServer server)
	{
		world(server, false);
		plugins(false);
		serverPassed = true;
		writeResult(Map.of("status", "PASS", "side", "server", "candidates", 3, "loaded", 2, "origin", jar(CommonTestPlugin.class), "world", server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString()));
		PLibDatabase.LOGGER.info("PULSE BOOTSTRAP SERVER COMPLETE");
	}

	public static void serverTick(MinecraftServer server)
	{
		if (serverPassed && server.getTickCount() >= 20)
			server.halt(false);
	}

	public static void serverStopped()
	{
		check(serverPassed, "Packaged bootstrap server completed before normal stop");
		PLibDatabase.LOGGER.info("PULSE BOOTSTRAP SERVER SHUTDOWN PASS");
	}

	public static void writeResult(Map<String, ?> result)
	{
		try
		{
			Files.writeString(runDirectory().resolve("bootstrap-results.json"), new GsonBuilder().
					setPrettyPrinting().create().toJson(result));
		}
		catch (IOException exception)
		{
			throw new IllegalStateException(exception);
		}
	}

	public static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("PULSE BOOTSTRAP: " + message);
	}
}
