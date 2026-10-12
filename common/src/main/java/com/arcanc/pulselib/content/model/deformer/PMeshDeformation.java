/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import java.util.Objects;

public record PMeshDeformation(PDeformerStack stack, PDeformerValueSource values, Object cacheKey, int subdivisionLevel)
{
	public static final int DEFAULT_SUBDIVISION_LEVEL = 2;

	public PMeshDeformation(PDeformerStack stack, PDeformerValueSource values, Object cacheKey)
	{
		this(stack, values, cacheKey, DEFAULT_SUBDIVISION_LEVEL);
	}

	public PMeshDeformation
	{
		Objects.requireNonNull(stack);
		Objects.requireNonNull(values);
		Objects.requireNonNull(cacheKey);
		if (subdivisionLevel < 0 || subdivisionLevel > PMeshTessellator.MAX_SUBDIVISION_LEVEL)
			throw new IllegalArgumentException("Subdivision level must be in [0, " + PMeshTessellator.MAX_SUBDIVISION_LEVEL + "]: " + subdivisionLevel);
	}
}
