package com.arcanc.pulselib.content.model;

/** Immutable material reference for one mesh primitive. */
public record PMaterial(String textureReference)
{
	public PMaterial
	{
		textureReference = PTextureReference.normalize(textureReference);
	}
}
