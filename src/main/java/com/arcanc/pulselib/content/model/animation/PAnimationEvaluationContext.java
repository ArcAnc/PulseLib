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

public final class PAnimationEvaluationContext
{
	private final MolangParser.Context molang;
	private final float animationTime;
	private final Vector3f temporaryVector = new Vector3f();
	private final Quaternionf temporaryQuaternion = new Quaternionf();

	/**
	 * Creates an instance of the enclosing type.
	 * @param molang the molang to use.
	 */
	public PAnimationEvaluationContext(MolangParser.Context molang)
	{
		this(molang, 0f);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param molang the molang to use.
	 * @param animationTime the animation time to use.
	 */
	public PAnimationEvaluationContext(MolangParser.Context molang, float animationTime)
	{
		this.molang = Objects.requireNonNull(molang);
		this.animationTime = animationTime;
	}

	/**
	 * Performs the molang operation.
	 * @return the value produced by this operation.
	 */
	public MolangParser.Context molang()
	{
		return this.molang;
	}

	/**
	 * Performs the temporary vector operation.
	 * @return the value produced by this operation.
	 */
	public Vector3f temporaryVector()
	{
		return this.temporaryVector;
	}

	/**
	 * Performs the temporary quaternion operation.
	 * @return the value produced by this operation.
	 */
	public Quaternionf temporaryQuaternion()
	{
		return this.temporaryQuaternion;
	}

	/**
	 * Performs the this values operation.
	 * @param value the value to use.
	 */
	public void thisValues(Vector3f value)
	{
		this.molang.thisValues(value.x(), value.y(), value.z());
	}

	/**
	 * Performs the evaluate operation.
	 * @param expression the expression to use.
	 * @return the value produced by this operation.
	 */
	public float evaluate(MolangParser.Expression expression)
	{
		return PExpressionEvaluator.SHARED.evaluate(expression, this.molang, this.animationTime);
	}
}
