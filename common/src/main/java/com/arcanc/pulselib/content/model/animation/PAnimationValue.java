/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

/**
 * Supplies a constant or expression-backed value for a typed animation keyframe.
 *
 * @param <T> the sampled animation channel value type
 */
@FunctionalInterface
public interface PAnimationValue<T>
{
	/**
	 * Evaluates this keyframe value into the supplied mutable destination.
	 *
	 * @param context the animation sampling context and reusable scratch values
	 * @param destination the mutable object receiving the result
	 */
	void evaluate(PAnimationEvaluationContext context, T destination);

	/**
	 * Returns whether the value is independent of the evaluation context.
	 *
	 * @return whether the value is independent of the evaluation context
	 */
	default boolean isConstant()
	{
		return false;
	}
}
