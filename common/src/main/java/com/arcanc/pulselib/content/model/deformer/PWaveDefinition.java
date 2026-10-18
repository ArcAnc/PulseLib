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
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3f;

/**
 * Serializable configuration and animation channel references for wave displacement along a configurable
 * axis.
 *
 * @param origin the local pivot or origin of the deformation
 * @param lengthAxis the axis along which the deformation extent is measured
 * @param displacementAxis the axis along which the wave displaces vertices
 * @param positiveExtent the positive-axis deformation extent
 * @param negativeExtent the negative-axis deformation extent
 * @param wavelength the local distance between repeated wave crests
 * @param amplitude the amplitude channel reference and its fallback value
 * @param phase the phase channel reference and its fallback value
 */
public record PWaveDefinition(Vector3f origin,
                              Vector3f lengthAxis,
                              Vector3f displacementAxis,
							  float positiveExtent,
							  float negativeExtent,
							  float wavelength,
							  PChannelReference<Float> amplitude,
							  PChannelReference<Float> phase)
{
	/**
	 * Serialization codec for wave definition.
	 */
	public static final MapCodec<PWaveDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.
			group(
					PLibCodecs.VECTOR3F_CODEC.fieldOf("origin").forGetter(PWaveDefinition::origin),
					PLibCodecs.VECTOR3F_CODEC.fieldOf("length_axis").forGetter(PWaveDefinition::lengthAxis),
					PLibCodecs.VECTOR3F_CODEC.fieldOf("displacement_axis").forGetter(PWaveDefinition::displacementAxis),
					Codec.FLOAT.fieldOf("positive_extent").forGetter(PWaveDefinition::positiveExtent),
					Codec.FLOAT.fieldOf("negative_extent").forGetter(PWaveDefinition::negativeExtent),
					Codec.FLOAT.fieldOf("wavelength").forGetter(PWaveDefinition::wavelength),
					PChannelReference.FLOAT.fieldOf("amplitude").forGetter(PWaveDefinition::amplitude),
					PChannelReference.FLOAT.fieldOf("phase").forGetter(PWaveDefinition::phase)).
			apply(instance, PWaveDefinition::new));

	/**
	 * Creates the deformation configuration and copies its local origin and axis inputs.
	 *
	 * @param origin the local pivot or origin of the deformation
	 * @param lengthAxis the axis along which the deformation extent is measured
	 * @param displacementAxis the axis along which the wave displaces vertices
	 * @param positiveExtent the positive-axis deformation extent
	 * @param negativeExtent the negative-axis deformation extent
	 * @param wavelength the local distance between repeated wave crests
	 * @param amplitude the amplitude channel reference and its fallback value
	 * @param phase the phase channel reference and its fallback value
	 */
	public PWaveDefinition
	{
		origin = new Vector3f(origin);
		lengthAxis = new Vector3f(lengthAxis);
		displacementAxis = new Vector3f(displacementAxis);
		if (positiveExtent < 0.0f || negativeExtent < 0.0f || positiveExtent + negativeExtent <= 0.0f)
			throw new IllegalArgumentException("Wave extents must describe a non-empty interval");
		if (wavelength <= 0.0f)
			throw new IllegalArgumentException("Wave wavelength must be positive");
	}
}
