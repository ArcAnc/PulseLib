/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.rhi;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Loads actual production/Minecraft assets with the native import preprocessor, without a game ResourceManager. */
final class RhiProductionShaderSource
{
	static String load(Identifier id, ShaderType type)
	{
		String path = "shaders/" + id.getPath() + (type == ShaderType.VERTEX ? ".vsh" : ".fsh");
		var preprocessor = new GlslPreprocessor()
		{
			private final Set<Identifier> imported = new HashSet<>();

			@Override
			public @Nullable String applyImport(boolean relative, String path)
			{
				if (relative)
					throw new IllegalArgumentException("Unexpected relative import in basic production shaders: " + path);
				Identifier include = Identifier.parse(path);
				if (!this.imported.add(include))
					return null;
				return read(include.getNamespace(), "shaders/include/" + include.getPath());
			}
		};
		return String.join("", preprocessor.process(read(id.getNamespace(), path)));
	}

	private static String read(String namespace, String path)
	{
		String resource = "/assets/" + namespace + "/" + path;
		try (var stream = RhiProductionShaderSource.class.getResourceAsStream(resource))
		{
			if (stream == null)
				throw new IllegalStateException("Missing production shader resource " + resource);
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot read production shader resource " + resource, exception);
		}
	}
}
