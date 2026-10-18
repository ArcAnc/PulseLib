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
 * Typed animation channel tracks associated with one source-model bone.
 *
 * @param boneUuid the bone uuid
 * @param tracks the tracks
 */
public record PBoneAnimation(UUID boneUuid, Map<String, PAnimationTrack<?>> tracks)
{
	/**
	 * Returns the track registered for the supplied channel type, or {@code null} if absent.
	 *
	 * @param <T> the value type carried by this contract
	 * @param type the registered animation channel type
	 * @return the track registered for the supplied channel type, or {@code null} if absent
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
	 * Returns whether this bone animation contains the supplied channel type.
	 *
	 * @param type the registered animation channel type
	 * @return whether this bone animation contains the supplied channel type
	 */
	public boolean hasChannel(PAnimationChannelType<?> type)
	{
		return track(type) != null;
	}
}
