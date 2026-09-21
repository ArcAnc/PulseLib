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
 * Provides support for root motion runtime.
 */
public final class PRootMotionRuntime
{
	private final PAnimation animation;
	private final String rootBoneName;
	private final PInterpolationType interpolation;
	private final Object data;

	/**
	 * Creates an instance of the enclosing type.
	 * @param animation the animation to use.
	 * @param rootBoneName the root bone name to use.
	 * @param interpolation the interpolation to use.
	 */
	public PRootMotionRuntime(PAnimation animation, String rootBoneName, PInterpolationType interpolation)
	{
		this(animation, rootBoneName, interpolation, null);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param animation the animation to use.
	 * @param rootBoneName the root bone name to use.
	 * @param interpolation the interpolation to use.
	 * @param data the data to use.
	 */
	public PRootMotionRuntime(PAnimation animation, String rootBoneName, PInterpolationType interpolation, Object data)
	{
		this.animation = Objects.requireNonNull(animation, "animation");
		this.rootBoneName = Objects.requireNonNull(rootBoneName, "rootBoneName");
		this.interpolation = Objects.requireNonNull(interpolation, "interpolation");
		this.data = data;
	}

	/**
	 * Extracts the root motion.
	 * @param previousTime the previous time to use.
	 * @param currentTime the current time to use.
	 * @return the value produced by this operation.
	 */
	public PRootMotionDelta extractRootMotion(float previousTime, float currentTime)
	{
		return PAnimationRuntime.extractRootMotion(this.animation, this.rootBoneName,
				previousTime, currentTime, this.interpolation, this.data);
	}
}
