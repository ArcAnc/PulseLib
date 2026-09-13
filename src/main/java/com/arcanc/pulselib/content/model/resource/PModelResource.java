/**
 * @author ArcAnc
 * Created at: 13.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.resource;


import com.arcanc.pulselib.content.model.PTextureReference;
import com.arcanc.pulselib.data.PModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object representing model resource.
 */
public record PModelResource(
		Identifier model,
		Identifier modelLoaderId,
		Map<String, Identifier> textures)
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
		textures = Map.copyOf(normalizedTextures);
	}

	public static Builder builder(Identifier model)
	{
		Objects.requireNonNull(model);
		return new Builder(model);
	}

	/**
	 * Resolves this resource's model id through its selected loader.
	 *
	 * @param loader the loader selected for this resource.
	 * @return an equivalent resource with a normalized model location.
	 * @throws IllegalArgumentException when {@code loader} does not match this resource's loader id.
	 */
	public PModelResource normalizeModelLocation(PModelLoader loader)
	{
		Objects.requireNonNull(loader);
		if (!this.modelLoaderId.equals(loader.id()))
			throw new IllegalArgumentException("Model loader does not match resource loader id: " + this.modelLoaderId);
		return new PModelResource(loader.normalizeModelResourceLocation(this.model), this.modelLoaderId, this.textures);
	}

/**
 * Builds builder.
 */
	public static class Builder
	{
		public final Identifier model;
		public Identifier modelLoader = PGltfModelLoader.INSTANCE.id();
		public final Map<String, Identifier> textures = new LinkedHashMap<>();

		public Builder(Identifier model)
		{
			Objects.requireNonNull(model);
			this.model = model;
		}

		public Builder modelLoader(Identifier modelLoader)
		{
			Objects.requireNonNull(modelLoader);
			this.modelLoader = modelLoader;
			return this;
		}

		public Builder texture(String key, Identifier texture)
		{
			Objects.requireNonNull(key);
			Objects.requireNonNull(texture);
			this.textures.put(PTextureReference.normalize(key), texture);
			return this;
		}

		public PModelResource build()
		{
			return new PModelResource(this.model, this.modelLoader, this.textures);
		}
	}
}
