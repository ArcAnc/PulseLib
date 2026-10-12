/**
 * @author ArcAnc
 * Created at: 24.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


import org.jetbrains.annotations.Nullable;

/**
 * Resolved command produced by a stage player.
 */
public record PAnimationStagePlayback(
		@Nullable String animationName,
		@Nullable PAnimationType animationType,
		PInterpolationType interpolationType,
		float speed,
		int waitTicks
)
{
	public PAnimationStagePlayback
	{
		if (interpolationType == null)
			throw new IllegalArgumentException("Interpolation type must not be null");
		if (!Float.isFinite(speed))
			throw new IllegalArgumentException("Animation speed must be finite");
		if (waitTicks < 0)
			throw new IllegalArgumentException("Wait ticks must be non-negative");
		if (animationName == null && animationType != null)
			throw new IllegalArgumentException("Wait playback must not have an animation type");
		if (animationName != null && (animationName.isBlank() || animationType == null))
			throw new IllegalArgumentException("Clip playback needs a name and an animation type");
		if (animationName != null && waitTicks != 0)
			throw new IllegalArgumentException("Clip playback must not have wait ticks");
	}

	/**
	 * Creates a clip playback command.
	 * @param animationName the animation name to use.
	 * @param animationType the playback type to use.
	 * @param interpolationType the interpolation to use.
	 * @param speed the speed to use.
	 * @return the playback command.
	 */
	public static PAnimationStagePlayback clip(String animationName, PAnimationType animationType,
	                                           PInterpolationType interpolationType, float speed)
	{
		return new PAnimationStagePlayback(animationName, animationType, interpolationType, speed, 0);
	}

	/**
	 * Creates a wait playback command.
	 * @param ticks the duration in ticks.
	 * @return the playback command.
	 */
	public static PAnimationStagePlayback waitFor(int ticks)
	{
		return new PAnimationStagePlayback(null, null, PInterpolationType.STEP, 1.0f, ticks);
	}

	/**
	 * Determines whether this command waits without playing an animation.
	 * @return whether this command is a wait.
	 */
	public boolean isWaiting()
	{
		return this.animationName == null;
	}
}
