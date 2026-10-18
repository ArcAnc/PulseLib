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
import com.arcanc.pulselib.content.model.animation.PVectorConversion;
import org.joml.Vector3f;

/**
 * Three Molang expressions sampled component-wise and converted to animation vector coordinates.
 */
public final class PMolangVectorValue implements PAnimationValue<Vector3f>
{
	private final MolangParser.Expression x;
	private final MolangParser.Expression y;
	private final MolangParser.Expression z;
	private final PVectorConversion conversion;

	/**
	 * Creates a component-wise Molang vector value with source-format coordinate conversion.
	 *
	 * @param x the Molang expression for the x component
	 * @param y the Molang expression for the y component
	 * @param z the Molang expression for the z component
	 * @param conversion the conversion
	 */
	public PMolangVectorValue(MolangParser.Expression x, MolangParser.Expression y, MolangParser.Expression z, PVectorConversion conversion)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.conversion = conversion;
	}

	/**
	 * Evaluates the component expressions and writes the converted vector into the destination.
	 *
	 * @param context the animation sampling context and reusable scratch values
	 * @param destination the mutable object receiving the result
	 */
	@Override
	public void evaluate(PAnimationEvaluationContext context, Vector3f destination)
	{
		destination.set(
				evaluateComponent(this.x, context, 0),
				evaluateComponent(this.y, context, 1),
				evaluateComponent(this.z, context, 2));
		this.conversion.apply(destination);
	}

	private static float evaluateComponent(MolangParser.Expression expression, PAnimationEvaluationContext context, int component)
	{
		context.molang().thisValue(context.molang().thisComponent(component));
		return context.evaluate(expression);
	}
}
