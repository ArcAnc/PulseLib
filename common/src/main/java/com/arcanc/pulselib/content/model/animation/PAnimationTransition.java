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

public record PAnimationTransition(
		int source,
		int target,
		PCondition condition,
		float exitTime,
		float blendDuration,
		int priority,
		PInterruptionPolicy interruption
)
{
	
	public PAnimationTransition(int source,
	                            int target,
	                            PCondition condition,
	                            float blendDuration,
	                            int priority,
	                            PInterruptionPolicy interruption)
	{
		this(source, target, condition, -1.0f, blendDuration, priority, interruption);
	}

	public PAnimationTransition
	{
		Objects.requireNonNull(condition);
		Objects.requireNonNull(interruption);
		if (source < 0 || target < 0)
			throw new IllegalArgumentException("Animation transition state indices must be non-negative");
		if (exitTime < -1.0f || exitTime > 1.0f)
			throw new IllegalArgumentException("Exit time must be in [0, 1], or -1 when unused");
		if (blendDuration < 0.0f)
			throw new IllegalArgumentException("Blend duration must be non-negative");
	}
}
