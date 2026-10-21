/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PExpressionEvaluator;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Molang evaluation state and reusable vector/quaternion scratch values for animation track sampling.
 */
public final class PAnimationEvaluationContext
{
	private final MolangParser.Context molang;
	private final float animationTime;
	private final Vector3f temporaryVector = new Vector3f();
	private final Quaternionf temporaryQuaternion = new Quaternionf();

	/**
	 * Creates a sampling context around the supplied Molang values.
	 *
	 * @param molang the Molang values used for this animation sample
	 */
	public PAnimationEvaluationContext(MolangParser.Context molang)
	{
		this(molang, 0f);
	}

	/**
	 * @param molang frame queries (including anim_time in seconds) and persistent variables.
	 * @param animationTime clip sampling time in ticks; supplied queries are not converted again.
	 */
	public PAnimationEvaluationContext(MolangParser.Context molang, float animationTime)
	{
		this.molang = Objects.requireNonNull(molang);
		this.animationTime = animationTime;
	}

	/**
	 * Converts interpolated controller/clip ticks to Molang seconds exactly once.
	 *
	 * @param ticks the duration in game ticks
	 * @return the supplied tick position divided by twenty
	 */
	public static float ticksToSeconds(float ticks)
	{
		return ticks / 20.0f;
	}

	/**
	 * Returns the Molang context used to resolve animation expressions.
	 *
	 * @return the Molang context used to resolve animation expressions
	 */
	public MolangParser.Context molang()
	{
		return this.molang;
	}

	/**
	 * Returns a reusable vector scratch value for the current evaluation.
	 *
	 * @return a reusable vector scratch value for the current evaluation
	 */
	public Vector3f temporaryVector()
	{
		return this.temporaryVector;
	}

	/**
	 * Returns a reusable quaternion scratch value for the current evaluation.
	 *
	 * @return a reusable quaternion scratch value for the current evaluation
	 */
	public Quaternionf temporaryQuaternion()
	{
		return this.temporaryQuaternion;
	}

	/**
	 * Updates component-wise Molang {@code this} values from the accumulated vector.
	 *
	 * @param value the value written by this operation
	 */
	public void thisValues(Vector3f value)
	{
		this.molang.thisValues(value.x(), value.y(), value.z());
	}

	/**
	 * Evaluates an expression against the current animation and Molang context.
	 *
	 * @param expression the expression
	 * @return the expression value evaluated with the current Molang inputs
	 */
	public float evaluate(MolangParser.Expression expression)
	{
		return PExpressionEvaluator.SHARED.evaluate(expression, this.molang, this.animationTime);
	}
}
