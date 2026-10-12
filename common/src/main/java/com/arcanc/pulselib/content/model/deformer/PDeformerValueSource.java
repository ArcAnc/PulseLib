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

@FunctionalInterface
public interface PDeformerValueSource
{
	PDeformerValueSource DEFAULTS = PChannelReference :: defaultValue;

	float resolve(PChannelReference<Float> reference);

	static PDeformerValueSource parameters(PAnimationParameters parameters)
	{
		return reference -> reference.name().isEmpty() ? reference.defaultValue() :
				parameters.get(reference.name(), reference.defaultValue());
	}
}
