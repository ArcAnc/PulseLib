package com.arcanc.pulselib.content.model.resource;

import com.arcanc.pulselib.content.model.PTextureReference;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable registration of a model loader and its material textures. */
public record PModelResource(Identifier model, Identifier modelLoaderId, Map<String, Identifier> textures)
{
	public PModelResource
	{
		Objects.requireNonNull(model);
		Objects.requireNonNull(modelLoaderId);
		Map<String, Identifier> normalizedTextures = new LinkedHashMap<>();
		textures.forEach((reference, texture) ->
		{
			String normalizedReference = PTextureReference.normalize(reference);
			if (normalizedTextures.putIfAbsent(normalizedReference, Objects.requireNonNull(texture)) != null)
				throw new IllegalArgumentException("Duplicate texture reference for model " + model + ": " + normalizedReference);
		});
		textures = Collections.unmodifiableMap(normalizedTextures);
	}
}
