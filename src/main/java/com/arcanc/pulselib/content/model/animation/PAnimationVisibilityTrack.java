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

public record PAnimationVisibilityTrack(List<Keyframe> keyframes)
{
	public PAnimationVisibilityTrack
	{
		keyframes = keyframes.stream().
				sorted(Comparator.comparingDouble(Keyframe :: time)).
				toList();
	}

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

	public record Keyframe(float time, boolean visible)
	{
	}
}
