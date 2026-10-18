/**
 * @author ArcAnc
 * Created at: 22.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;

import java.util.Objects;

/** Normalizes texture references stored by model formats. */
public final class PTextureReference
{
	private static final String PNG_EXTENSION = ".png";

	private PTextureReference()
	{
	}

	/**
	 * Converts a source texture reference into the canonical override lookup form.
	 *
	 * @param reference the reference value or channel used by this operation
	 * @return the normalized texture override lookup key
	 */
	public static String normalize(String reference)
	{
		Objects.requireNonNull(reference);
		return reference.endsWith(PNG_EXTENSION) ?
				reference.substring(0, reference.length() - PNG_EXTENSION.length()) : reference;
	}
}
