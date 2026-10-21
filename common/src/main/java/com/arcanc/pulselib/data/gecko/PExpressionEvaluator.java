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
 * Evaluates against the current context on every call. Even TIME_ONLY queries can
 * be overridden by a context's resolver; clip time alone is never a cache key.
 * Parsed CONSTANT programs already retain their folded value in MolangParser.
 */
public final class PExpressionEvaluator
{
	/**
	 * Shared evaluator for expressions whose caching follows the dependency contract.
	 */
	public static final PExpressionEvaluator SHARED = new PExpressionEvaluator();

	/**
	 * Creates an expression evaluator that retains only context-independent constant results.
	 */
	public PExpressionEvaluator()
	{
		this(1f / 20f);
	}

	/**
	 * Retained for source/binary compatibility. Time is no longer quantized.
	 * @param timeQuantum the formerly used positive cache quantum.
	 */
	public PExpressionEvaluator(float timeQuantum)
	{
		if (timeQuantum <= 0f)
		{
			throw new IllegalArgumentException("timeQuantum must be positive");
		}
	}

	/**
	 * @param expression the parsed program.
	 * @param context the actual queries, variables, resolver and random state.
	 * @param animationTime clip time in ticks, retained for API compatibility.
	 * @return the current result, without sharing values between contexts.
	 */
	public float evaluate(MolangParser.Expression expression, MolangParser.Context context, float animationTime)
	{
		return expression.evaluate(context);
	}

	/** Retained for compatibility with resource reload callers; no values are retained. */
	public void clearTimeCache()
	{
	}
}
