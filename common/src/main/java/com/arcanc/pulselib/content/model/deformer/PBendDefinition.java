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

public record PBendDefinition(Vector3f origin,
							 Vector3f lengthAxis,
							 Vector3f bendAxis,
							 float positiveExtent,
							 float negativeExtent,
							 PChannelReference<Float> angle)
{
	public static final MapCodec<PBendDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.
			group(
					PLibCodecs.VECTOR3F_CODEC.fieldOf("origin").forGetter(PBendDefinition::origin),
					PLibCodecs.VECTOR3F_CODEC.fieldOf("length_axis").forGetter(PBendDefinition::lengthAxis),
					PLibCodecs.VECTOR3F_CODEC.fieldOf("bend_axis").forGetter(PBendDefinition::bendAxis),
					Codec.FLOAT.fieldOf("positive_extent").forGetter(PBendDefinition::positiveExtent),
					Codec.FLOAT.fieldOf("negative_extent").forGetter(PBendDefinition::negativeExtent),
					PChannelReference.FLOAT_CODEC.forGetter(PBendDefinition::angle)).
			apply(instance, PBendDefinition::new));

	public PBendDefinition
	{
		origin = new Vector3f(origin);
		lengthAxis = new Vector3f(lengthAxis);
		bendAxis = new Vector3f(bendAxis);
		if (positiveExtent < 0.0f || negativeExtent < 0.0f || positiveExtent + negativeExtent <= 0.0f)
			throw new IllegalArgumentException("Bend extents must describe a non-empty interval");
	}
}
