/**
 * @author ArcAnc
 * Created at: 13.08.2026
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
 * Serializable configuration and animation channel references for rigid hinge rotation around a pivot.
 *
 * @param origin the local pivot or origin of the deformation
 * @param lengthAxis the axis along which the deformation extent is measured
 * @param hingeAxis the axis around which the rigid hinge rotates
 * @param angle the angle channel reference and its fallback value
 */
public record PHingeDefinition(Vector3f origin, Vector3f lengthAxis, Vector3f hingeAxis,
                               PChannelReference<Float> angle)
{
	/**
	 * Serialization codec for hinge definition.
	 */
	public static final MapCodec<PHingeDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			PLibCodecs.VECTOR3F_CODEC.fieldOf("origin").forGetter(PHingeDefinition::origin),
			PLibCodecs.VECTOR3F_CODEC.fieldOf("length_axis").forGetter(PHingeDefinition::lengthAxis),
			PLibCodecs.VECTOR3F_CODEC.fieldOf("hinge_axis").forGetter(PHingeDefinition::hingeAxis),
			PChannelReference.FLOAT_CODEC.forGetter(PHingeDefinition::angle)
	).apply(instance, PHingeDefinition::new));

	/**
	 * Creates the deformation configuration and copies its local origin and axis inputs.
	 *
	 * @param origin the local pivot or origin of the deformation
	 * @param lengthAxis the axis along which the deformation extent is measured
	 * @param hingeAxis the axis around which the rigid hinge rotates
	 * @param angle the angle channel reference and its fallback value
	 */
	public PHingeDefinition
	{
		origin = new Vector3f(origin);
		lengthAxis = new Vector3f(lengthAxis);
		hingeAxis = new Vector3f(hingeAxis);
	}
}
