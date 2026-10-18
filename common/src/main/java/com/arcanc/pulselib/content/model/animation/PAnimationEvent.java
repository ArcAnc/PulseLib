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

/**
 * Typed event payload scheduled at an animation position measured in game ticks.
 *
 * @param <T> the decoded animation event payload type
 * @param time the event position in animation game ticks
 * @param type the registered animation event type
 * @param data the decoded event payload
 */
public record PAnimationEvent<T>(float time, PAnimationEventType<T> type, T data)
{
	/**
	 * Creates a typed event scheduled at a finite, non-negative animation tick position.
	 *
	 * @param time the event position in animation game ticks
	 * @param type the registered animation event type
	 * @param data the decoded event payload
	 */
	public PAnimationEvent
	{
		if (!Float.isFinite(time) || time < 0.0f)
			throw new IllegalArgumentException("Animation event time must be a finite, non-negative value");
		Objects.requireNonNull(type, "Animation event type");
		Objects.requireNonNull(data, "Animation event data");
	}
}
