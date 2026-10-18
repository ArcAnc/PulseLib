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

/**
 * Prepared mesh deformation, sampled values, cache identity and tessellation level used for rendering.
 *
 * @param stack the item stack supplying render or animation state
 * @param values the source of current animated deformation channel values
 * @param cacheKey the identity used to reuse deformation results
 * @param subdivisionLevel the triangle subdivision level applied before deformation
 */
public record PMeshDeformation(PDeformerStack stack, PDeformerValueSource values, Object cacheKey, int subdivisionLevel)
{
	/**
	 * Default triangle subdivision level used before applying mesh deformation.
	 */
	public static final int DEFAULT_SUBDIVISION_LEVEL = 2;

	/**
	 * Creates a deformation submission with sampled values, cache identity and tessellation settings.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @param values the source of current animated deformation channel values
	 * @param cacheKey the identity used to reuse deformation results
	 */
	public PMeshDeformation(PDeformerStack stack, PDeformerValueSource values, Object cacheKey)
	{
		this(stack, values, cacheKey, DEFAULT_SUBDIVISION_LEVEL);
	}

	/**
	 * Creates a deformation submission with sampled values, cache identity and tessellation settings.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @param values the source of current animated deformation channel values
	 * @param cacheKey the identity used to reuse deformation results
	 * @param subdivisionLevel the triangle subdivision level applied before deformation
	 */
	public PMeshDeformation
	{
		Objects.requireNonNull(stack);
		Objects.requireNonNull(values);
		Objects.requireNonNull(cacheKey);
		if (subdivisionLevel < 0 || subdivisionLevel > PMeshTessellator.MAX_SUBDIVISION_LEVEL)
			throw new IllegalArgumentException("Subdivision level must be in [0, " + PMeshTessellator.MAX_SUBDIVISION_LEVEL + "]: " + subdivisionLevel);
	}
}
