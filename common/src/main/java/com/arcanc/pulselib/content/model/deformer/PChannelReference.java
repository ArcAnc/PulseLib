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

/**
 * Named animation parameter reference with a fallback value for mesh deformation.
 *
 * @param <T> the value type carried by this contract
 * @param name the animation parameter name, or the constant-reference name used by this channel
 * @param defaultValue the default value
 */
public record PChannelReference<T>(String name, T defaultValue)
{
	/**
	 * Serialization codec for channel reference.
	 */
	public static final MapCodec<PChannelReference<Float>> FLOAT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.
			group(
					Codec.STRING.optionalFieldOf("channel", "").forGetter(PChannelReference::name),
				Codec.FLOAT.optionalFieldOf("default", 0.0f).forGetter(PChannelReference::defaultValue)).
			apply(instance, PChannelReference::new));
	/**
	 * Serialization codec for channel reference.
	 */
	public static final Codec<PChannelReference<Float>> FLOAT = FLOAT_CODEC.codec();

	/**
	 * Creates a named channel reference and its fallback value.
	 *
	 * @param name the animation parameter name, or the constant-reference name used by this channel
	 * @param defaultValue the default value
	 */
	public PChannelReference
	{
		if (name == null)
			throw new IllegalArgumentException("Channel name cannot be null");
	}

	/**
	 * Creates a float channel reference that always uses the supplied fallback value.
	 *
	 * @param value the value written by this operation
	 * @return the channel reference carrying the constant fallback value
	 */
	public static PChannelReference<Float> constant(float value)
	{
		return new PChannelReference<>("", value);
	}
}
