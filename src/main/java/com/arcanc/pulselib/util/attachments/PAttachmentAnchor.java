/**
 * @author ArcAnc
 * Created at: 04.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import net.minecraft.resources.Identifier;

public record PAttachmentAnchor(Identifier id)
{
	/**
	 * Performs the of operation.
	 * @param id the id to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentAnchor of(Identifier id)
	{
		return new PAttachmentAnchor(id);
	}
	
	/**
	 * Performs the minecraft operation.
	 * @param path the path to use.
	 * @return the value produced by this operation.
	 */
	public static PAttachmentAnchor minecraft(String path)
	{
		return of(Identifier.withDefaultNamespace(path));
	}
}
