/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Accumulates temporary animation-triggered camera shake contributions.
 */
public final class PAnimationCameraShake
{
	private static final List<Shake> SHAKES = new ArrayList<>();

	private PAnimationCameraShake() { }

	/**
	 * Adds a camera shake contribution with the supplied strength, duration and frequency.
	 *
	 * @param strength the strength
	 * @param duration the duration of the effect or transition in game ticks
	 * @param frequency the frequency
	 */
	public static void add(float strength, float duration, float frequency)
	{
		if (Float.isFinite(strength) && Float.isFinite(duration) && Float.isFinite(frequency) && strength != 0.0f && duration > 0.0f)
			SHAKES.add(new Shake(Math.abs(strength), duration, Math.max(frequency, 0.0f), 0.0f));
	}

	/**
	 * Samples the combined camera shake at the interpolated tick position.
	 *
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the combined camera shake angle at the interpolated tick position
	 */
	public static synchronized float sample(float partialTick)
	{
		float result = 0.0f;
		for (Shake shake : SHAKES)
		{
			float progress = Math.min((shake.age + partialTick) / shake.duration, 1.0f);
			result += shake.strength * (1.0f - progress) * (float)Math.sin((shake.age + partialTick) * shake.frequency);
		}
		return result;
	}

	/**
	 * Advances active shake contributions and removes those whose duration has elapsed.
	 */
	public static synchronized void tick()
	{
		for (Iterator<Shake> iterator = SHAKES.iterator(); iterator.hasNext(); )
		{
			Shake shake = iterator.next();
			shake.age++;
			if (shake.age >= shake.duration)
				iterator.remove();
		}
	}

	/**
	 * Playback state for one temporary camera shake contribution.
	 */
	private static final class Shake
	{
		private final float strength, duration, frequency;
		private float age;
		private Shake(float strength, float duration, float frequency, float age)
		{
			this.strength = strength;
			this.duration = duration;
			this.frequency = frequency;
			this.age = age;
		}
	}
}
