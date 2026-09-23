/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;

import java.util.Objects;

/** Normalizes texture names used by model materials and registrations. */
public final class PTextureReference
{
	private static final String PNG_EXTENSION = ".png";

	private PTextureReference()
	{
	}

	public static String normalize(String reference)
	{
		Objects.requireNonNull(reference);
		return reference.endsWith(PNG_EXTENSION) ?
				reference.substring(0, reference.length() - PNG_EXTENSION.length()) : reference;
	}
}
