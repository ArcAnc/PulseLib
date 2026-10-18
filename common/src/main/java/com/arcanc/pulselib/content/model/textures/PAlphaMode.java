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

/**
 * Alpha handling policy selected explicitly or inferred from texture contents.
 */
public enum PAlphaMode
{
	/**
	 * Determines alpha handling from texture pixels.
	 */
	AUTO,
	/**
	 * Renders the texture without alpha rejection or blending.
	 */
	OPAQUE,
	/**
	 * Rejects transparent pixels without blended translucency.
	 */
	CUTOUT,
	/**
	 * Renders partially transparent pixels with translucency.
	 */
	TRANSLUCENT;

	/**
	 * Serialization codec for alpha mode.
	 */
	public static final Codec<PAlphaMode> CODEC = Codec.STRING.comapFlatMap(PAlphaMode :: decode, PAlphaMode :: serializedName);

	/**
	 * Returns the lowercase identifier used to serialize this alpha policy.
	 *
	 * @return the lowercase identifier used to serialize this alpha policy
	 */
	public String serializedName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

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
