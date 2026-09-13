/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides support for expression evaluator.
 */
public final class PExpressionEvaluator
{
	public static final PExpressionEvaluator SHARED = new PExpressionEvaluator();

	private final Map<MolangParser.Expression, TimedValue> timeOnlyValues = new ConcurrentHashMap<>();
	private final float timeQuantum;

	/**
	 * Creates an instance of the enclosing type.
	 */
	public PExpressionEvaluator()
	{
		this(1f / 20f);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param timeQuantum the time quantum to use.
	 */
	public PExpressionEvaluator(float timeQuantum)
	{
		if (timeQuantum <= 0f)
			throw new IllegalArgumentException("timeQuantum must be positive");
		this.timeQuantum = timeQuantum;
	}

	/**
	 * Performs the evaluate operation.
	 * @param expression the expression to use.
	 * @param context the context to use.
	 * @param animationTime the animation time to use.
	 * @return the value produced by this operation.
	 */
	public float evaluate(MolangParser.Expression expression, MolangParser.Context context, float animationTime)
	{
		return switch (expression.dependency())
		{
			case CONSTANT, INSTANCE, STATEFUL -> expression.evaluate(context);
			case TIME_ONLY -> timeOnly(expression, context, Math.round(animationTime / this.timeQuantum));
		};
	}

	/**
	 * Clears the time cache.
	 */
	public void clearTimeCache()
	{
		this.timeOnlyValues.clear();
	}

	/**
	 * Performs the time only operation.
	 * @param expression the expression to use.
	 * @param context the context to use.
	 * @param timeStep the time step to use.
	 * @return the value produced by this operation.
	 */
	private float timeOnly(MolangParser.Expression expression, MolangParser.Context context, int timeStep)
	{
		TimedValue cached = this.timeOnlyValues.get(expression);
		if (cached != null && cached.timeStep == timeStep)
			return cached.value;
		float value = expression.evaluate(context);
		this.timeOnlyValues.put(expression, new TimedValue(timeStep, value));
		return value;
	}

/**
 * Immutable value object representing timed value.
 */
	private record TimedValue(int timeStep, float value)
	{
	}
}
