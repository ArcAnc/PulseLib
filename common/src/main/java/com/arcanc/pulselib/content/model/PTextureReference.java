/**
 * @author ArcAnc
 * Created at: 13.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;


import java.util.Objects;

/**
 * Provides support for texture reference.
 */
public final class PTextureReference
{
	private static final String PNG_EXTENSION = ".png";

	private PTextureReference()
	{
	}

	/**
	 * Normalizes a model texture reference.
	 * @param reference the reference from a model or resource registration.
	 * @return the reference without a terminal {@code .png} extension.
	 */
	public static String normalize(String reference)
	{
		Objects.requireNonNull(reference);
		return reference.endsWith(PNG_EXTENSION) ?
				reference.substring(0, reference.length() - PNG_EXTENSION.length()) : reference;
	}
}
