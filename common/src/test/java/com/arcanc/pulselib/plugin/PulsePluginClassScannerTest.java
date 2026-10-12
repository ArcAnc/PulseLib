package com.arcanc.pulselib.plugin;

import com.arcanc.pulselib.api.plugin.PulsePlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.ByteArrayInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PulsePluginClassScannerTest
{
	@TempDir Path directory;

	@Test
	void readsDefaultSideWithoutResolvingMissingClientSuperclass() throws Exception
	{
		var scanner = new PulsePluginClassScanner();
		var candidate = scanner.read("example", new ByteArrayInputStream(bytecode(null)));
		assertEquals("example.ClientPlugin", candidate.className());
		assertEquals("example", candidate.modId());
		assertEquals(PulsePlugin.Side.CLIENT, candidate.side());
	}

	@Test
	void scansJarDirectoryAndLoaderOwnedZipFilesystem() throws Exception
	{
		Path classes = this.directory.resolve("build/classes/java/main");
		Files.createDirectories(classes.resolve("example"));
		Files.write(classes.resolve("example/ClientPlugin.class"), bytecode("BOTH"));
		Path jar = this.directory.resolve("nested-mod.jar");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar)))
		{
			output.putNextEntry(new JarEntry("example/ClientPlugin.class"));
			output.write(bytecode("BOTH"));
			output.closeEntry();
		}
		var scanner = new PulsePluginClassScanner();
		var expected = scanner.scan("example", classes);
		assertEquals(1, expected.size());
		assertEquals(PulsePlugin.Side.BOTH, expected.getFirst().side());
		assertEquals(expected, scanner.scan("example", jar));
		try (var filesystem = FileSystems.newFileSystem(jar, Map.of()))
		{
			assertEquals(expected, scanner.scan("example", filesystem.getPath("/")));
			assertTrue(filesystem.isOpen(), "Scanning must not close the loader's filesystem");
		}
	}

	@Test
	void rejectsUnknownSide() throws Exception
	{
		assertThrows(IllegalArgumentException.class, () -> new PulsePluginClassScanner().read(
				"example", new ByteArrayInputStream(bytecode("INVALID"))));
	}

	private static byte[] bytecode(String side)
	{
		ClassWriter writer = new ClassWriter(0);
		writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "example/ClientPlugin", null, "missing/client/OnlyOnClient", null);
		var annotation = writer.visitAnnotation(Type.getDescriptor(PulsePlugin.class), true);
		if (side != null)
			annotation.visitEnum("value", Type.getDescriptor(PulsePlugin.Side.class), side);
		annotation.visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}
}
