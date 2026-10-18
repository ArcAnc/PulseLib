/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.arcanc.pulselib.content.model.animation.PAnimationParameters;

/**
 * Resolves animated float values for mesh deformer channel references.
 */
@FunctionalInterface
public interface PDeformerValueSource
{
	/**
	 * Resolves every channel to its declared fallback value.
	 */
	PDeformerValueSource DEFAULTS = PChannelReference :: defaultValue;

	/**
	 * Returns the current value of the supplied channel, falling back to its default value.
	 *
	 * @param reference the reference channel reference and its fallback value
	 * @return the current value of the supplied channel, falling back to its default value
	 */
	float resolve(PChannelReference<Float> reference);

	/**
	 * Creates a value source backed by animation graph parameters.
	 *
	 * @param parameters the graph parameter and trigger state
	 * @return the source of current animated deformation channel values
	 */
	static PDeformerValueSource parameters(PAnimationParameters parameters)
	{
		return reference -> reference.name().isEmpty() ? reference.defaultValue() :
				parameters.get(reference.name(), reference.defaultValue());
	}
}
