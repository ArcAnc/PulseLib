/**
 * @author ArcAnc
 * Created at: 21.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.textures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum PAlphaMode
{
	AUTO,
	OPAQUE,
	CUTOUT,
	TRANSLUCENT;

	public static final Codec<PAlphaMode> CODEC = Codec.STRING.comapFlatMap(PAlphaMode :: decode, PAlphaMode :: serializedName);

	/**
	 * Performs the serialized name operation.
	 * @return the value produced by this operation.
	 */
	public String serializedName()
	{
		return this.name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Performs the decode operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	private static DataResult<PAlphaMode> decode(String name)
	{
		try
		{
			return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
		}
		catch (IllegalArgumentException exception)
		{
			return DataResult.error(() -> "Unknown PulseLib alpha mode: " + name);
		}
	}
}
