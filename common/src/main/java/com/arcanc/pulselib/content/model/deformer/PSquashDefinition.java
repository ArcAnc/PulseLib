/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.arcanc.pulselib.util.helpers.PLibCodecs;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3f;

/**
 * Serializable configuration and animation channel references for squashing along a configurable axis.
 *
 * @param origin the local pivot or origin of the deformation
 * @param axis the normalized deformation axis
 * @param scale the scale channel reference and its fallback value
 */
public record PSquashDefinition(Vector3f origin, Vector3f axis, PChannelReference<Float> scale)
{
	/**
	 * Serialization codec for squash definition.
	 */
	public static final MapCodec<PSquashDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.
			group(
					PLibCodecs.VECTOR3F_CODEC.fieldOf("origin").forGetter(PSquashDefinition::origin),
					PLibCodecs.VECTOR3F_CODEC.fieldOf("axis").forGetter(PSquashDefinition::axis),
					PChannelReference.FLOAT.fieldOf("scale").forGetter(PSquashDefinition::scale)).
			apply(instance, PSquashDefinition::new));

	/**
	 * Creates the deformation configuration and copies its local origin and axis inputs.
	 *
	 * @param origin the local pivot or origin of the deformation
	 * @param axis the normalized deformation axis
	 * @param scale the scale channel reference and its fallback value
	 */
	public PSquashDefinition
	{
		origin = new Vector3f(origin);
		axis = new Vector3f(axis);
	}
}
