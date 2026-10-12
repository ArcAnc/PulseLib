/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import java.util.Objects;

public record PAnimationSample(String animation, float weight)
{
	public PAnimationSample
	{
		animation = Objects.requireNonNull(animation);
		if (animation.isBlank())
			throw new IllegalArgumentException("Animation name must not be blank");
		if (weight < 0.0f)
			throw new IllegalArgumentException("Animation sample weight must be non-negative");
	}
}
