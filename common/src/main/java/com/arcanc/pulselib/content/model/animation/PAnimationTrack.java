/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import java.util.List;

/**
 * Immutable keyframe list for one typed animation channel.
 *
 * @param <T> the sampled animation channel value type
 * @param channel the typed channel controlling interpolation and pose application
 * @param keyframes the channel keyframes in animation time order
 */
public record PAnimationTrack<T>(PAnimationChannelType<T> channel, List<PKeyframe<T>> keyframes)
{
	/**
	 * Copies the keyframes of one typed animation channel into immutable storage.
	 *
	 * @param channel the typed channel controlling interpolation and pose application
	 * @param keyframes the channel keyframes in animation time order
	 */
	public PAnimationTrack
	{
		keyframes = List.copyOf(keyframes);
	}
}
