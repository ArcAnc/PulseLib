/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PBendDeformer implements PMeshDeformer<PBendDefinition>
{
	public static final PBendDeformer INSTANCE = new PBendDeformer();
	private static final float EPSILON = 1.0e-5f;

	private PBendDeformer()
	{
	}

	@Override
	public ResourceLocation id()
	{
		return PLibDatabase.rl("bend");
	}

	@Override
	public MapCodec<PBendDefinition> codec()
	{
		return PBendDefinition.CODEC;
	}

	@Override
	public void prepare(PDeformerPrepareContext context, PBendDefinition definition)
	{
		Vector3f length = requireUnit(definition.lengthAxis(), "lengthAxis");
		Vector3f axis = new Vector3f(definition.bendAxis()).fma(-definition.bendAxis().dot(length), length);
		if (axis.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("bendAxis must not be parallel to lengthAxis");
		axis.normalize();
		Vector3f radial = new Vector3f(axis).cross(length).normalize();
		context.add(new Operation(new Vector3f(definition.origin()), length, axis, radial,
				definition.positiveExtent(), definition.negativeExtent(), definition.angle()));
	}

	private static Vector3f requireUnit(Vector3f axis, String name)
	{
		Vector3f result = new Vector3f(axis);
		if (result.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException(name + " must not be zero");
		return result.normalize();
	}

	private record Operation(Vector3f origin, Vector3f length, Vector3f axis, Vector3f radial,
							 float positiveExtent, float negativeExtent, PChannelReference<Float> angle)
			implements PPreparedDeformer
	{
		@Override
		public void deform(Vector3f position, PDeformerValueSource values)
		{
			float totalLength = this.positiveExtent + this.negativeExtent;
			float totalAngle = values.resolve(this.angle);
			if (Math.abs(totalAngle) < EPSILON)
				return;

			Vector3f relative = new Vector3f(position).sub(this.origin);
			float along = relative.dot(this.length);
			float radialOffset = relative.dot(this.radial);
			float axialOffset = relative.dot(this.axis);
			float clampedAlong = Math.clamp(along, -this.negativeExtent, this.positiveExtent);
			float curvature = totalAngle / totalLength;
			float radius = 1.0f / curvature;
			float theta = curvature * clampedAlong;
			Quaternionf rotation = new Quaternionf().fromAxisAngleRad(this.axis, theta);

			// C - R(theta) * (C - u*N) produces an arc with tangent lengthAxis at origin.
			Vector3f center = new Vector3f(this.radial).mul(radius);
			Vector3f bent = new Vector3f(center).sub(rotation.transform(new Vector3f(center).fma(-radialOffset, this.radial)));
			Vector3f tangent = rotation.transform(new Vector3f(this.length));
			bent.fma(along - clampedAlong, tangent).fma(axialOffset, this.axis).add(this.origin);
			position.set(bent);
		}
	}
}
