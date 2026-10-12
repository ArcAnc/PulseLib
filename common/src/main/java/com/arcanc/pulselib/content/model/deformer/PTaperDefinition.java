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

public record PTaperDefinition(Vector3f origin, Vector3f lengthAxis, float positiveExtent, float negativeExtent,
								PChannelReference<Float> tipScale)
{
	public static final MapCodec<PTaperDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.
			group(
					PLibCodecs.VECTOR3F_CODEC.fieldOf("origin").forGetter(PTaperDefinition::origin),
					PLibCodecs.VECTOR3F_CODEC.fieldOf("length_axis").forGetter(PTaperDefinition::lengthAxis),
					Codec.FLOAT.fieldOf("positive_extent").forGetter(PTaperDefinition::positiveExtent),
					Codec.FLOAT.fieldOf("negative_extent").forGetter(PTaperDefinition::negativeExtent),
					PChannelReference.FLOAT.fieldOf("tip_scale").forGetter(PTaperDefinition::tipScale)).
			apply(instance, PTaperDefinition::new));

	public PTaperDefinition
	{
		origin = new Vector3f(origin);
		lengthAxis = new Vector3f(lengthAxis);
		if (positiveExtent < 0.0f || negativeExtent < 0.0f || positiveExtent + negativeExtent <= 0.0f)
			throw new IllegalArgumentException("Taper extents must describe a non-empty interval");
	}
}
