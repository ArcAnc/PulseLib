/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * Prepares CPU mesh deformation operations for squashing along a configurable axis.
 */
public final class PSquashDeformer implements PMeshDeformer<PSquashDefinition>
{
	/**
	 * Shared instance of this stateless implementation.
	 */
	public static final PSquashDeformer INSTANCE = new PSquashDeformer();
	private static final float EPSILON = 1.0e-5f;

	private PSquashDeformer()
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
		return PLibDatabase.rl("squash");
	}

	/**
	 * Returns the codec for this deformer's configuration.
	 *
	 * @return the codec for this deformer's configuration
	 */
	@Override
	public MapCodec<PSquashDefinition> codec()
	{
		return PSquashDefinition.CODEC;
	}

	/**
	 * Compiles the supplied definition into operations appended to the preparation context.
	 *
	 * @param context the destination collecting prepared deformation operations
	 * @param definition the immutable configuration to register or evaluate
	 */
	@Override
	public void prepare(PDeformerPrepareContext context, PSquashDefinition definition)
	{
		Vector3f axis = new Vector3f(definition.axis());
		if (axis.lengthSquared() < EPSILON * EPSILON)
			throw new IllegalArgumentException("axis must not be zero");
		context.add(new Operation(new Vector3f(definition.origin()), axis.normalize(), definition.scale()));
	}

	/**
	 * Prepared mesh operation for squashing along a configurable axis.
	 *
	 * @param origin the local pivot or origin of the deformation
	 * @param axis the normalized deformation axis
	 * @param scale the scale channel reference and its fallback value
	 */
	private record Operation(Vector3f origin, Vector3f axis, PChannelReference<Float> scale) implements PPreparedDeformer
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
			float axialScale = Math.max(values.resolve(this.scale), EPSILON);
			float radialScale = 1.0f / (float)Math.sqrt(axialScale);
			Vector3f relative = new Vector3f(position).sub(this.origin);
			Vector3f axial = new Vector3f(this.axis).mul(relative.dot(this.axis) * axialScale);
			Vector3f radial = relative.sub(new Vector3f(this.axis).mul(relative.dot(this.axis))).mul(radialScale);
			position.set(axial.add(radial).add(this.origin));
		}
	}
}
