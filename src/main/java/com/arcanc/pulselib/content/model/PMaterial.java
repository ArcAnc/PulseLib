/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model;

/** Immutable material reference for one mesh primitive. */
public record PMaterial(String textureReference)
{
	public PMaterial
	{
		textureReference = PTextureReference.normalize(textureReference);
	}
}
