package com.arcanc.pulselib.fabric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FabricDevelopmentRootsTest
{
	@TempDir Path directory;

	@Test
	void includesSeparatedCompiledRootsButExcludesUnrelatedClasspathGroups() throws Exception
	{
		Path resources = Files.createDirectories(this.directory.resolve("mod/build/resources/main"));
		Path classes = Files.createDirectories(this.directory.resolve("mod/build/classes/java/main"));
		Path ideClasses = Files.createDirectories(this.directory.resolve("ide/production/mod"));
		Path unrelated = Files.createDirectories(this.directory.resolve("unrelated"));
		String groups = resources + File.pathSeparator + ideClasses + File.pathSeparator + File.pathSeparator + unrelated;
		Set<Path> result = FabricPulsePluginFinder.developmentRoots(Set.of(resources), groups);
		assertTrue(result.contains(classes));
		assertTrue(result.contains(ideClasses));
		assertFalse(result.contains(unrelated));
	}
}
