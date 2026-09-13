/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;

public enum PExpressionDependency
{
	CONSTANT,
	TIME_ONLY,
	INSTANCE,
	STATEFUL;

	/**
	 * Performs the combine operation.
	 * @param first the first to use.
	 * @param second the second to use.
	 * @return the value produced by this operation.
	 */
	public static PExpressionDependency combine(PExpressionDependency first, PExpressionDependency second)
	{
		return first.ordinal() >= second.ordinal() ? first : second;
	}
}
