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

/**
 * Immutable value object representing animation transition.
 */
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

	/**
	 * Creates an instance of the enclosing type.
	 * @param source the source to use.
	 * @param target the target to use.
	 * @param condition the condition to use.
	 * @param blendDuration the blend duration to use.
	 * @param priority the priority to use.
	 * @param interruption the interruption to use.
	 */
	public PAnimationTransition(int source,
	                            int target,
	                            PCondition condition,
	                            float blendDuration,
	                            int priority,
	                            PInterruptionPolicy interruption)
	{
		this(source, target, condition, -1.0f, blendDuration, priority, interruption);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param source the source to use.
	 * @param target the target to use.
	 * @param condition the condition to use.
	 * @param exitTime the exit time to use.
	 * @param blendDuration the blend duration to use.
	 * @param priority the priority to use.
	 * @param interruption the interruption to use.
	 */
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
