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

public final class PExpressionEvaluator
{
	public static final PExpressionEvaluator SHARED = new PExpressionEvaluator();

	private final Map<MolangParser.Expression, TimedValue> timeOnlyValues = new ConcurrentHashMap<>();
	private final float timeQuantum;

	public PExpressionEvaluator()
	{
		this(1f / 20f);
	}

	public PExpressionEvaluator(float timeQuantum)
	{
		if (timeQuantum <= 0f)
			throw new IllegalArgumentException("timeQuantum must be positive");
		this.timeQuantum = timeQuantum;
	}

	public float evaluate(MolangParser.Expression expression, MolangParser.Context context, float animationTime)
	{
		return switch (expression.dependency())
		{
			case CONSTANT, INSTANCE, STATEFUL -> expression.evaluate(context);
			case TIME_ONLY -> timeOnly(expression, context, Math.round(animationTime / this.timeQuantum));
		};
	}

	public void clearTimeCache()
	{
		this.timeOnlyValues.clear();
	}

	private float timeOnly(MolangParser.Expression expression, MolangParser.Context context, int timeStep)
	{
		TimedValue cached = this.timeOnlyValues.get(expression);
		if (cached != null && cached.timeStep == timeStep)
			return cached.value;
		float value = expression.evaluate(context);
		this.timeOnlyValues.put(expression, new TimedValue(timeStep, value));
		return value;
	}

	private record TimedValue(int timeStep, float value)
	{
	}
}
