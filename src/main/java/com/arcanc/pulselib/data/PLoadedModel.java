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
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * A parsed model and the physical resource selected by its loader.
 */
public record PLoadedModel(Identifier modelId, Identifier source, PModel model)
{
	public PLoadedModel
	{
		Objects.requireNonNull(modelId);
		Objects.requireNonNull(source);
		Objects.requireNonNull(model);
	}
}
