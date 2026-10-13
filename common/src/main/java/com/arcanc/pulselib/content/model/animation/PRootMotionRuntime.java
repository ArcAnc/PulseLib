/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Extracts animation-space motion for callers. PulseLib applies ROOT to visual
 * poses only; using the extracted delta for entity movement, collisions or
 * server synchronization is the consuming mod's responsibility.
 */
public final class PRootMotionRuntime
{
	private final PAnimation animation;
	private final String rootBoneName;
	private final PInterpolationType interpolation;
	private final @Nullable Object data;

	public PRootMotionRuntime(PAnimation animation, String rootBoneName, PInterpolationType interpolation)
	{
		this(animation, rootBoneName, interpolation, null);
	}

	public PRootMotionRuntime(PAnimation animation, String rootBoneName, PInterpolationType interpolation, @Nullable Object data)
	{
		this.animation = Objects.requireNonNull(animation, "animation");
		this.rootBoneName = Objects.requireNonNull(rootBoneName, "rootBoneName");
		this.interpolation = Objects.requireNonNull(interpolation, "interpolation");
		this.data = data;
	}
	
	public PRootMotionDelta extractRootMotion(float previousTime, float currentTime)
	{
		return PAnimationRuntime.extractRootMotion(this.animation, this.rootBoneName,
				previousTime, currentTime, this.interpolation, this.data);
	}
}
