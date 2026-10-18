/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;

/**
 * Conservative classification of the context state an expression depends on.
 */
public enum PExpressionDependency
{
	/**
	 * Depends on no mutable context values.
	 */
	CONSTANT,
	/**
	 * Depends on current animation time and must be evaluated with the current context.
	 */
	TIME_ONLY,
	/**
	 * Depends on instance, frame or query values.
	 */
	INSTANCE,
	/**
	 * Reads or changes persistent variables, random state or evaluation order.
	 */
	STATEFUL;

	/**
	 * Returns the dependency classification that includes both supplied dependencies.
	 *
	 * @param first the first expression dependency
	 * @param second the second expression dependency
	 * @return the dependency classification that includes both supplied dependencies
	 */
	public static PExpressionDependency combine(PExpressionDependency first, PExpressionDependency second)
	{
		return first.ordinal() >= second.ordinal() ? first : second;
	}
}
