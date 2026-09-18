package com.arcanc.pulselib.content.model.animation;
import java.util.Objects;
@FunctionalInterface
/**
 * Defines the contract for condition.
 */
public interface PCondition
{
	PCondition ALWAYS = parameters -> true;
	PCondition NEVER = parameters -> false;

	/**
	 * Performs the test operation.
	 * @param parameters the parameters to use.
	 * @return the value produced by this operation.
	 */
	boolean test(PAnimationParameters parameters);
	
	/**
	 * Performs the consume operation.
	 * @param parameters the parameters to use.
	 */
	default void consume(PAnimationParameters parameters)
	{
	}

	/**
	 * Performs the parameter operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	static PCondition parameter(String name)
	{
		return parameters -> parameters.getBoolean(name);
	}

	/**
	 * Performs the greater than operation.
	 * @param name the name to use.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	static PCondition greaterThan(String name, float value)
	{
		return parameters -> parameters.get(name) > value;
	}

	/**
	 * Performs the greater or equal operation.
	 * @param name the name to use.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	static PCondition greaterOrEqual(String name, float value)
	{
		return parameters -> parameters.get(name) >= value;
	}

	/**
	 * Performs the less than operation.
	 * @param name the name to use.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	static PCondition lessThan(String name, float value)
	{
		return parameters -> parameters.get(name) < value;
	}

	/**
	 * Performs the triggered operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	static PCondition triggered(String name)
	{
		Objects.requireNonNull(name);
		return new PCondition()
		{
			/**
			 * Performs the test operation.
			 * @param parameters the parameters to use.
			 * @return the value produced by this operation.
			 */
			@Override
			public boolean test(PAnimationParameters parameters)
			{
				return parameters.isTriggered(name);
			}

			/**
			 * Performs the consume operation.
			 * @param parameters the parameters to use.
			 */
			@Override
			public void consume(PAnimationParameters parameters)
			{
				parameters.consumeTrigger(name);
			}
		};
	}
}
