/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.plugin;

import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.util.PLibDatabase;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads bytecode only. Roots must be supplied by the mod loader.
 */
public final class PulsePluginClassScanner
{
	/**
	 * JVM descriptor of the plugin annotation recognized by the bytecode scanner.
	 */
	public static final String ANNOTATION_DESCRIPTOR = Type.getDescriptor(PulsePlugin.class);

	/**
	 * Finds annotated plugin classes below a directory, archive or loader-owned filesystem root.
	 *
	 * @param modId the owning mod identifier used in discovery diagnostics
	 * @param root the root
	 * @return the plugin candidates discovered below the supplied root
	 * @throws IOException if the source resource cannot be read
	 */
	public List<PulsePluginCandidate> scan(String modId, Path root) throws IOException
	{
		if (Files.isRegularFile(root) && root.toString().endsWith(".jar"))
			try (var archive = FileSystems.newFileSystem(root))
			{
				return scan(modId, archive.getPath("/"));
			}
		List<PulsePluginCandidate> result = new ArrayList<>();
		try (var paths = Files.walk(root))
		{
			var iterator = paths.filter(path -> path.toString().endsWith(".class") && Files.isRegularFile(path)).
					iterator();
			while (iterator.hasNext())
			{
				Path file = iterator.next();
				try (InputStream input = Files.newInputStream(file))
				{
					PulsePluginCandidate candidate = read(modId, input);
					if (candidate != null)
						result.add(candidate);
				}
				catch (IOException | RuntimeException exception)
				{
					if (modId.equals(PLibDatabase.MOD_ID))
						throw new IOException("Cannot scan internal PulseLib class " + file, exception);
					PLibDatabase.LOGGER.error("Cannot scan PulseLib plugin bytecode {} from mod {}", file, modId, exception);
				}
			}
		}
		return List.copyOf(result);
	}

	/**
	 * Reads one class stream and returns its plugin annotation data, or {@code null} if it is not a plugin.
	 *
	 * @param modId the owning mod identifier used in discovery diagnostics
	 * @param input the class bytecode input stream
	 * @return the discovered plugin metadata, or {@code null} for an unannotated class
	 * @throws IOException if the source resource cannot be read
	 */
	public @Nullable PulsePluginCandidate read(String modId, InputStream input) throws IOException
	{
		PluginVisitor visitor = new PluginVisitor();
		new ClassReader(input).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		return visitor.annotated ? new PulsePluginCandidate(visitor.className, modId, visitor.side) : null;
	}

	/**
	 * ASM visitor that captures a class name and its PulseLib plugin annotation without loading the class.
	 */
	private static final class PluginVisitor extends ClassVisitor
	{
		private @Nullable String className;
		private boolean annotated;
		private PulsePlugin.Side side = PulsePlugin.Side.CLIENT;

		private PluginVisitor()
		{
			super(Opcodes.ASM9);
		}

		/**
		 * Captures the binary class name from the class-file header without resolving the class.
		 *
		 * @param version the version
		 * @param access the access
		 * @param name the name
		 * @param signature the signature; may be {@code null}
		 * @param superName the super name; may be {@code null}
		 * @param interfaces the interfaces; may be {@code null}
		 */
		@Override
		public void visit(int version, int access, String name, @Nullable String signature, @Nullable String superName, @Nullable String[] interfaces)
		{
			this.className = name.replace('/', '.');
		}

		/**
		 * Recognizes the PulseLib plugin annotation and reads its physical-side restriction.
		 *
		 * @param descriptor the descriptor
		 * @param visible whether the selected bone is visible
		 * @return the computed {@code AnnotationVisitor} value, or {@code null} when the requested value is unavailable
		 */
		@Override
		public @Nullable AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
		{
			if (!ANNOTATION_DESCRIPTOR.equals(descriptor))
				return null;
			this.annotated = true;
			return new AnnotationVisitor(Opcodes.ASM9)
			{
				@Override
				public void visitEnum(String name, String enumDescriptor, String value)
				{
					if (name.equals("value"))
					{
						if (!enumDescriptor.equals(Type.getDescriptor(PulsePlugin.Side.class)))
							throw new IllegalArgumentException("Invalid @PulsePlugin side type on " + className);
						side = PulsePlugin.Side.valueOf(value);
					}
				}
			};
		}
	}
}
