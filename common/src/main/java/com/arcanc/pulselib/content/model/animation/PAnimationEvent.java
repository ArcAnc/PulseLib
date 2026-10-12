/**
 * @author ArcAnc
 * Created at: 27.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */
package com.arcanc.pulselib.content.model.animation;

import java.util.Objects;

public record PAnimationEvent<T>(float time, PAnimationEventType<T> type, T data)
{
	public PAnimationEvent
	{
		if (!Float.isFinite(time) || time < 0.0f)
			throw new IllegalArgumentException("Animation event time must be a finite, non-negative value");
		Objects.requireNonNull(type, "Animation event type");
		Objects.requireNonNull(data, "Animation event data");
	}
}
