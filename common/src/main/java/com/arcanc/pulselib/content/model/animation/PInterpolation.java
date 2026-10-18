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
 * Maps interpolation progress to the weight used when sampling an animation channel.
 */
@FunctionalInterface
public interface PInterpolation
{
	/**
	 * Transforms normalized keyframe progress into an interpolation weight.
	 *
	 * @param strength the strength
	 * @return the transformed interpolation weight
	 */
	double buildTransformer(double strength);
}
