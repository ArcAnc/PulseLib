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

public record PTwistDefinition(Vector3f origin,
							  Vector3f lengthAxis,
							  float positiveExtent,
							  float negativeExtent,
							  PChannelReference<Float> angle)
{
	public static final MapCodec<PTwistDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			PLibCodecs.VECTOR3F_CODEC.fieldOf("origin").forGetter(PTwistDefinition::origin),
			PLibCodecs.VECTOR3F_CODEC.fieldOf("length_axis").forGetter(PTwistDefinition::lengthAxis),
			Codec.FLOAT.fieldOf("positive_extent").forGetter(PTwistDefinition::positiveExtent),
			Codec.FLOAT.fieldOf("negative_extent").forGetter(PTwistDefinition::negativeExtent),
			PChannelReference.FLOAT_CODEC.forGetter(PTwistDefinition::angle)
	).apply(instance, PTwistDefinition::new));

	public PTwistDefinition
	{
		origin = new Vector3f(origin);
		lengthAxis = new Vector3f(lengthAxis);
		if (positiveExtent < 0.0f || negativeExtent < 0.0f || positiveExtent + negativeExtent <= 0.0f)
			throw new IllegalArgumentException("Twist extents must describe a non-empty interval");
	}
}
