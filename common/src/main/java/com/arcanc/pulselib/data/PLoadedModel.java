/**
 * @author ArcAnc
 * Created at: 27.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data;

import com.arcanc.pulselib.content.model.PModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * A model resolved by a loader. {@code modelId} is the canonical cache key;
 * {@code source} is the physical resource used for parsing and sidecars.
 *
 * @param modelId the canonical model identifier
 * @param source the physical resource selected by the model loader
 * @param model the parsed source-model hierarchy and geometry
 */
public record PLoadedModel(ResourceLocation modelId, ResourceLocation source, PModel model)
{
	/**
	 * Associates a parsed model with its canonical id and selected physical source resource.
	 *
	 * @param modelId the canonical model identifier
	 * @param source the physical resource selected by the model loader
	 * @param model the parsed source-model hierarchy and geometry
	 */
	public PLoadedModel
	{
		Objects.requireNonNull(modelId);
		Objects.requireNonNull(source);
		Objects.requireNonNull(model);
	}
}
