package com.arcanc.pulselib.content.model;

/** Immutable material reference for a model primitive. */
public record PMaterial(String textureReference)
{
	public PMaterial
	{
		textureReference = PTextureReference.normalize(textureReference);
	}
}
