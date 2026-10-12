/**
 * @author ArcAnc
 * Created at: 13.08.2026
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

public final class PHingeDeformer implements PMeshDeformer<PHingeDefinition>
{
	public static final PHingeDeformer INSTANCE = new PHingeDeformer();
	private static final float EPSILON = 1.0e-5f;

	private PHingeDeformer()
	{
	}

	@Override
	public ResourceLocation id()
	{
		return PLibDatabase.rl("hinge");
	}

	@Override
	public MapCodec<PHingeDefinition> codec()
	{
		return PHingeDefinition.CODEC;
	}

	@Override
	public void prepare(PDeformerPrepareContext context, PHingeDefinition definition)
	{
		Vector3f length = unit(definition.lengthAxis(), "lengthAxis");
		Vector3f hinge = new Vector3f(definition.hingeAxis()).fma(-definition.hingeAxis().dot(length), length);
		if (hinge.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("hingeAxis must not be parallel to lengthAxis");
		context.add(new Operation(new Vector3f(definition.origin()), length, hinge.normalize(), definition.angle()));
	}

	private static Vector3f unit(Vector3f axis, String name)
	{
		Vector3f result = new Vector3f(axis);
		if (result.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException(name + " must not be zero");
		return result.normalize();
	}

	private record Operation(Vector3f origin, Vector3f length, Vector3f hinge,
							 PChannelReference<Float> angle) implements PPreparedDeformer
	{
		@Override
		public void deform(Vector3f position, PDeformerValueSource values)
		{
			Vector3f relative = new Vector3f(position).sub(this.origin);
			if (relative.dot(this.length) <= 0.0f)
				return;
			position.set(new Quaternionf().fromAxisAngleRad(this.hinge, values.resolve(this.angle)).transform(relative).add(this.origin));
		}
	}
}
