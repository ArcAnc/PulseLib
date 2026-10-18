/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

/**
 * Easing curves used to weight pose transitions.
 */
public enum PPoseEasing
{
	/**
	 * Uses unchanged transition progress.
	 */
	LINEAR
	{
		@Override public float apply(float value) { return value; }
	},
	/**
	 * Accelerates with a quadratic curve.
	 */
	EASE_IN_QUAD
	{
		@Override public float apply(float value) { return value * value; }
	},
	/**
	 * Decelerates with a quadratic curve.
	 */
	EASE_OUT_QUAD
	{
		@Override public float apply(float value) { return 1.0f - (1.0f - value) * (1.0f - value); }
	},
	/**
	 * Accelerates and decelerates with a cubic curve.
	 */
	EASE_IN_OUT_CUBIC
	{
		@Override public float apply(float value)
		{
			return value < 0.5f ? 4.0f * value * value * value : 1.0f - (float) Math.pow(-2.0f * value + 2.0f, 3.0) * 0.5f;
		}
	};

	/**
	 * Clamps transition progress and evaluates this easing curve.
	 *
	 * @param value the normalized interpolation or easing progress
	 * @return the eased transition weight
	 */
	public abstract float apply(float value);

	/**
	 * Evaluates this curve at the supplied normalized transition progress.
	 *
	 * @param value the normalized interpolation or easing progress
	 * @return the easing curve value
	 */
	public float transform(float value)
	{
		return apply(Math.clamp(value, 0.0f, 1.0f));
	}
}
