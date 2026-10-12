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

public record PSquashDefinition(Vector3f origin, Vector3f axis, PChannelReference<Float> scale)
{
	public static final MapCodec<PSquashDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.
			group(
					PLibCodecs.VECTOR3F_CODEC.fieldOf("origin").forGetter(PSquashDefinition::origin),
					PLibCodecs.VECTOR3F_CODEC.fieldOf("axis").forGetter(PSquashDefinition::axis),
					PChannelReference.FLOAT.fieldOf("scale").forGetter(PSquashDefinition::scale)).
			apply(instance, PSquashDefinition::new));

	public PSquashDefinition
	{
		origin = new Vector3f(origin);
		axis = new Vector3f(axis);
	}
}
