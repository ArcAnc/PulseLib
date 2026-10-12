/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.plugin.IPulsePluginFinder;
import com.arcanc.pulselib.plugin.PulsePluginCandidate;
import com.arcanc.pulselib.plugin.PulsePluginClassScanner;
import com.arcanc.pulselib.util.PLibDatabase;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class FabricPulsePluginFinder implements IPulsePluginFinder
{
	private @Nullable List<PulsePluginCandidate> candidates;

	@Override
	public synchronized List<PulsePluginCandidate> findPlugins()
	{
		if (this.candidates != null)
			return this.candidates;
		long start = System.nanoTime();
		FabricLoader loader = FabricLoader.getInstance();
		PulsePluginClassScanner scanner = new PulsePluginClassScanner();
		Set<PulsePluginCandidate> found = new LinkedHashSet<>();
		Set<Path> scanned = new HashSet<>();
		for (var mod : loader.getAllMods().stream().sorted(Comparator.comparing(value -> value.getMetadata().getId())).
				toList())
		{
			if (mod.getMetadata().getType().equals("builtin"))
				continue;
			String id = mod.getMetadata().getId();
			Set<Path> roots = new LinkedHashSet<>(mod.getRootPaths());
			if (loader.isDevelopmentEnvironment())
				roots.addAll(developmentRoots(roots, System.getProperty("fabric.classPathGroups", "")));
			for (Path root : roots)
			{
				root = root.toAbsolutePath().normalize();
				if (!scanned.add(root))
					continue;
				try
				{
					found.addAll(scanner.scan(id, root));
				}
				catch (IOException | RuntimeException exception)
				{
					if (id.equals(PLibDatabase.MOD_ID))
						throw new IllegalStateException("Cannot scan PulseLib mod root " + root, exception);
					PLibDatabase.LOGGER.error("Cannot scan PulseLib plugins in mod {} at {}", id, root, exception);
				}
			}
		}
		this.candidates = found.stream().sorted(Comparator.comparing(PulsePluginCandidate :: modId).
				thenComparing(PulsePluginCandidate :: className)).toList();
		PLibDatabase.LOGGER.info("PulseLib Fabric ASM scan: {} roots, {} candidates, {} ms", scanned.size(), this.candidates.size(),
			(System.nanoTime() - start) / 1_000_000);
		return this.candidates;
	}

	/**
	 * Only loader-declared path groups and compiled siblings of known mod resource roots.
	 */
	static Set<Path> developmentRoots(Set<Path> modRoots, String pathGroups)
	{
		Set<Path> known = new HashSet<>();
		modRoots.forEach(root -> known.add(root.toAbsolutePath().normalize()));
		Set<Path> result = new LinkedHashSet<>();
		for (String group : pathGroups.split(Pattern.quote(File.pathSeparator + File.pathSeparator)))
		{
			Set<Path> paths = new LinkedHashSet<>();
			for (String entry : group.split(Pattern.quote(File.pathSeparator)))
				if (!entry.isBlank())
					paths.add(Path.of(entry).toAbsolutePath().normalize());
			if (paths.stream().anyMatch(known :: contains))
				paths.stream().filter(Files :: isDirectory).forEach(result :: add);
		}
		for (Path root : known)
		{
			if (root.getFileSystem() != FileSystems.getDefault() || !Files.isDirectory(root))
				continue;
			// Gradle/IntelliJ commonly separate build/resources/main from build/classes/<language>/main.
			if (root.endsWith(Path.of("build", "resources", "main")))
			{
				Path build = root.getParent().getParent();
				for (String language : List.of("java", "kotlin", "scala"))
				{
					Path classes = build.resolve("classes").resolve(language).resolve("main");
					if (Files.isDirectory(classes))
						result.add(classes);
				}
			}
		}
		return result;
	}
}
