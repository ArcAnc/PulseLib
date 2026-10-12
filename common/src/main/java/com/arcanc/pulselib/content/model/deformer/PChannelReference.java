/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PChannelReference<T>(String name, T defaultValue)
{
	public static final MapCodec<PChannelReference<Float>> FLOAT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.
			group(
					Codec.STRING.optionalFieldOf("channel", "").forGetter(PChannelReference::name),
				Codec.FLOAT.optionalFieldOf("default", 0.0f).forGetter(PChannelReference::defaultValue)).
			apply(instance, PChannelReference::new));
	public static final Codec<PChannelReference<Float>> FLOAT = FLOAT_CODEC.codec();

	public PChannelReference
	{
		if (name == null)
			throw new IllegalArgumentException("Channel name cannot be null");
	}

	public static PChannelReference<Float> constant(float value)
	{
		return new PChannelReference<>("", value);
	}
}
