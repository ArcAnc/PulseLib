/**
 * @author ArcAnc
 * Created at: 22.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.resource;

import com.arcanc.pulselib.content.model.PTextureReference;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A registered model and its model-local material texture references.
 *
 * @param modelId the canonical model identifier
 * @param modelLoaderId the identifier of the loader responsible for this model
 * @param textures the texture overrides indexed by normalized reference
 */
public record PModelResource(ResourceLocation modelId,
                             ResourceLocation modelLoaderId,
                             Map<String, ResourceLocation> textures)
{
	/**
	 * Copies canonical model and loader identifiers together with normalized texture overrides.
	 *
	 * @param modelId the canonical model identifier
	 * @param modelLoaderId the identifier of the loader responsible for this model
	 * @param textures the texture overrides indexed by normalized reference
	 */
	public PModelResource
	{
		Objects.requireNonNull(modelId);
		Objects.requireNonNull(modelLoaderId);
		Objects.requireNonNull(textures);
		Map<String, ResourceLocation> normalized = new LinkedHashMap<>();
		textures.forEach((reference, texture) ->
		{
			String key = PTextureReference.normalize(reference);
			if (normalized.putIfAbsent(key, Objects.requireNonNull(texture)) != null)
				throw new IllegalArgumentException("Duplicate texture reference for model " + modelId + ": " + key);
		});
		textures = Collections.unmodifiableMap(normalized);
	}
}
