/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


import java.util.Comparator;
import java.util.List;

/**
 * Step-sampled visibility keyframes for an animated model bone.
 *
 * @param keyframes the channel keyframes in animation time order
 */
public record PAnimationVisibilityTrack(List<Keyframe> keyframes)
{
	/**
	 * Copies visibility keyframes into their sampled timeline representation.
	 *
	 * @param keyframes the channel keyframes in animation time order
	 */
	public PAnimationVisibilityTrack
	{
		keyframes = keyframes.stream().
				sorted(Comparator.comparingDouble(Keyframe :: time)).
				toList();
	}

	/**
	 * Samples step visibility at the supplied animation tick position.
	 *
	 * @param time the animation position in game ticks
	 * @return whether the visibility track is enabled at the supplied time
	 */
	public boolean visibleAt(float time)
	{
		boolean visible = true;
		for (Keyframe keyframe : this.keyframes)
		{
			if (keyframe.time() > time)
				break;
			visible = keyframe.visible();
		}
		return visible;
	}

/**
	 * Visibility change scheduled at an animation position measured in game ticks.
	 *
	 * @param time the animation position in game ticks
	 * @param visible whether the selected bone is visible
	 */
	public record Keyframe(float time, boolean visible)
	{
	}
}
