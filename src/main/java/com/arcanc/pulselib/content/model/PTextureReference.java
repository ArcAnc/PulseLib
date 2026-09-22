package com.arcanc.pulselib.content.model;

import java.util.Objects;

/** Normalizes texture references stored by model formats. */
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
