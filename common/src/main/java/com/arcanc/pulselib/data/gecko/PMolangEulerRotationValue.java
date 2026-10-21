/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;

import com.arcanc.pulselib.content.model.animation.PAnimationEvaluationContext;
import com.arcanc.pulselib.content.model.animation.PAnimationValue;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Gecko Euler-degree expressions sampled before conversion to a quaternion rotation.
 */
public final class PMolangEulerRotationValue implements PAnimationValue<Quaternionf>
{
	private final PMolangVectorValue eulerDegrees;

	/**
	 * Uses source Euler-degree expressions as a sampled quaternion rotation value.
	 *
	 * @param eulerDegrees the euler degrees
	 */
	public PMolangEulerRotationValue(PMolangVectorValue eulerDegrees)
	{
		this.eulerDegrees = eulerDegrees;
	}

	/**
	 * Evaluates authored degrees without normalizing away complete revolutions.
	 *
	 * @param context the animation sampling context and reusable scratch values
	 * @param destination the mutable object receiving the result
	 */
	public void evaluateEuler(PAnimationEvaluationContext context, Vector3f destination)
	{
		this.eulerDegrees.evaluate(context, destination);
	}

	/**
	 * Evaluates the source Euler expressions and converts the result into the destination quaternion.
	 *
	 * @param context the animation sampling context and reusable scratch values
	 * @param destination the mutable object receiving the result
	 */
	@Override
	public void evaluate(PAnimationEvaluationContext context, Quaternionf destination)
	{
		Vector3f temporary = context.temporaryVector();
		evaluateEuler(context, temporary);
		destination.rotationXYZ(
				(float) Math.toRadians(temporary.x),
				(float) Math.toRadians(temporary.y),
				(float) Math.toRadians(temporary.z));
	}
}
