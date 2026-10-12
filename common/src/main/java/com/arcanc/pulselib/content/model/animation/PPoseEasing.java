/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

public enum PPoseEasing
{
	LINEAR
	{
		@Override public float apply(float value) { return value; }
	},
	EASE_IN_QUAD
	{
		@Override public float apply(float value) { return value * value; }
	},
	EASE_OUT_QUAD
	{
		@Override public float apply(float value) { return 1.0f - (1.0f - value) * (1.0f - value); }
	},
	EASE_IN_OUT_CUBIC
	{
		@Override public float apply(float value)
		{
			return value < 0.5f ? 4.0f * value * value * value : 1.0f - (float) Math.pow(-2.0f * value + 2.0f, 3.0) * 0.5f;
		}
	};

	public abstract float apply(float value);

	public float transform(float value)
	{
		return apply(Math.clamp(value, 0.0f, 1.0f));
	}
}
