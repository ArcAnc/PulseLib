/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import java.util.Objects;

@FunctionalInterface
public interface PCondition
{
	PCondition ALWAYS = parameters -> true;
	PCondition NEVER = parameters -> false;

	boolean test(PAnimationParameters parameters);
	
	default void consume(PAnimationParameters parameters)
	{
	}

	static PCondition parameter(String name)
	{
		return parameters -> parameters.getBoolean(name);
	}

	static PCondition greaterThan(String name, float value)
	{
		return parameters -> parameters.get(name) > value;
	}

	static PCondition greaterOrEqual(String name, float value)
	{
		return parameters -> parameters.get(name) >= value;
	}

	static PCondition lessThan(String name, float value)
	{
		return parameters -> parameters.get(name) < value;
	}

	static PCondition triggered(String name)
	{
		Objects.requireNonNull(name);
		return new PCondition()
		{
			@Override
			public boolean test(PAnimationParameters parameters)
			{
				return parameters.isTriggered(name);
			}

			@Override
			public void consume(PAnimationParameters parameters)
			{
				parameters.consumeTrigger(name);
			}
		};
	}
}
