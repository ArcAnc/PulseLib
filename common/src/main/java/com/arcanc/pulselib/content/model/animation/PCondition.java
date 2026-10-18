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

/**
 * Transition condition that can test graph parameters and consume one-shot trigger state.
 */
@FunctionalInterface
public interface PCondition
{
	/**
	 * Condition that always permits a transition.
	 */
	PCondition ALWAYS = parameters -> true;
	/**
	 * Condition that never permits a transition.
	 */
	PCondition NEVER = parameters -> false;

	/**
	 * Tests whether the current graph parameters satisfy this condition.
	 *
	 * @param parameters the graph parameter and trigger state
	 * @return whether the current graph parameters satisfy this condition
	 */
	boolean test(PAnimationParameters parameters);
	
	/**
	 * Consumes transient parameter state associated with taking this transition.
	 *
	 * @param parameters the graph parameter and trigger state
	 */
	default void consume(PAnimationParameters parameters)
	{
	}

	/**
	 * Creates a condition that tests the named boolean graph parameter.
	 *
	 * @param name the graph parameter name
	 * @return the created condition that tests the named boolean graph parameter
	 */
	static PCondition parameter(String name)
	{
		return parameters -> parameters.getBoolean(name);
	}

	/**
	 * Creates a condition requiring the named parameter to exceed the supplied threshold.
	 *
	 * @param name the graph parameter name
	 * @param value the value written by this operation
	 * @return the created condition requiring the named parameter to exceed the supplied threshold
	 */
	static PCondition greaterThan(String name, float value)
	{
		return parameters -> parameters.get(name) > value;
	}

	/**
	 * Creates a condition requiring the named parameter to meet or exceed the supplied threshold.
	 *
	 * @param name the graph parameter name
	 * @param value the value written by this operation
	 * @return the created condition requiring the named parameter to meet or exceed the supplied threshold
	 */
	static PCondition greaterOrEqual(String name, float value)
	{
		return parameters -> parameters.get(name) >= value;
	}

	/**
	 * Creates a condition requiring the named parameter to be below the supplied threshold.
	 *
	 * @param name the graph parameter name
	 * @param value the value written by this operation
	 * @return the created condition requiring the named parameter to be below the supplied threshold
	 */
	static PCondition lessThan(String name, float value)
	{
		return parameters -> parameters.get(name) < value;
	}

	/**
	 * Creates a condition that tests and consumes the named graph trigger.
	 *
	 * @param name the graph parameter name
	 * @return the created condition that tests and consumes the named graph trigger
	 */
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
