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

public final class PMolangEulerRotationValue implements PAnimationValue<Quaternionf>
{
	private final PMolangVectorValue eulerDegrees;

	public PMolangEulerRotationValue(PMolangVectorValue eulerDegrees)
	{
		this.eulerDegrees = eulerDegrees;
	}

	@Override
	public void evaluate(PAnimationEvaluationContext context, Quaternionf destination)
	{
		Vector3f temporary = context.temporaryVector();
		this.eulerDegrees.evaluate(context, temporary);
		destination.rotationXYZ(
				(float) Math.toRadians(temporary.x),
				(float) Math.toRadians(temporary.y),
				(float) Math.toRadians(temporary.z));
	}
}
