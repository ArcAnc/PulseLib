/**
 * @author ArcAnc
 * Created at: 22.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;

/**
 * Immutable material reference for a model primitive.
 *
 * @param textureReference the source-model texture reference used for override lookup
 */
public record PMaterial(String textureReference)
{
	/**
	 * Creates a material using a normalized texture reference.
	 *
	 * @param textureReference the source-model texture reference used for override lookup
	 */
	public PMaterial
	{
		textureReference = PTextureReference.normalize(textureReference);
	}
}
