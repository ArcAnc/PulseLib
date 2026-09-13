/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable value object representing bone animation.
 */
public record PBoneAnimation(UUID boneUuid, Map<String, PAnimationTrack<?>> tracks)
{
	/**
	 * Performs the track operation.
	 * @param type the type to use.
	 * @return the value produced by this operation.
	 */
	@SuppressWarnings("unchecked")
	public @Nullable <T> PAnimationTrack<T> track(PAnimationChannelType<T> type)
	{
		for (PAnimationTrack<?> track : this.tracks.values())
			if (track.channel() == type)
				return (PAnimationTrack<T>) track;
		return null;
	}

	/**
	 * Determines whether the object has channel.
	 * @param type the type to use.
	 * @return the value produced by this operation.
	 */
	public boolean hasChannel(PAnimationChannelType<?> type)
	{
		return track(type) != null;
	}
}
