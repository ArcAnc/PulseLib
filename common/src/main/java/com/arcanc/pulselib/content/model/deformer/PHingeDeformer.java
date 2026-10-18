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

/**
 * Prepares CPU mesh deformation operations for rigid hinge rotation around a pivot.
 */
public final class PHingeDeformer implements PMeshDeformer<PHingeDefinition>
{
	/**
	 * Shared instance of this stateless implementation.
	 */
	public static final PHingeDeformer INSTANCE = new PHingeDeformer();
	private static final float EPSILON = 1.0e-5f;

	private PHingeDeformer()
	{
	}

	/**
	 * Returns the namespaced identifier of this mesh deformer type.
	 *
	 * @return the namespaced identifier of this mesh deformer type
	 */
	@Override
	public ResourceLocation id()
	{
		return PLibDatabase.rl("hinge");
	}

	/**
	 * Returns the codec for this deformer's configuration.
	 *
	 * @return the codec for this deformer's configuration
	 */
	@Override
	public MapCodec<PHingeDefinition> codec()
	{
		return PHingeDefinition.CODEC;
	}

	/**
	 * Compiles the supplied definition into operations appended to the preparation context.
	 *
	 * @param context the destination collecting prepared deformation operations
	 * @param definition the immutable configuration to register or evaluate
	 */
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

	/**
	 * Prepared mesh operation for rigid hinge rotation around a pivot.
	 *
	 * @param origin the local pivot or origin of the deformation
	 * @param length the animation clip length in game ticks
	 * @param hinge the hinge
	 * @param angle the angle channel reference and its fallback value
	 */
	private record Operation(Vector3f origin, Vector3f length, Vector3f hinge,
							 PChannelReference<Float> angle) implements PPreparedDeformer
	{
		/**
		 * Applies this operation to the supplied position in place using current channel values.
		 *
		 * @param position the position
		 * @param values the source of current animated deformation channel values
		 */
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
