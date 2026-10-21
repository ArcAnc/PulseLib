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
 * Immutable value object representing wave definition.
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
	 * Creates an instance of the enclosing type.
	 * @param origin the origin to use.
	 * @param lengthAxis the length axis to use.
	 * @param displacementAxis the displacement axis to use.
	 * @param positiveExtent the positive extent to use.
	 * @param negativeExtent the negative extent to use.
	 * @param wavelength the wavelength to use.
	 * @param amplitude the amplitude to use.
	 * @param phase the phase to use.
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
