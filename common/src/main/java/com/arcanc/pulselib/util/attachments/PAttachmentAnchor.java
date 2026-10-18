/**
 * @author ArcAnc
 * Created at: 04.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import net.minecraft.resources.ResourceLocation;

/**
 * Namespaced identifier of a living-entity model attachment point.
 *
 * @param id the namespaced resource or registration identifier
 */
public record PAttachmentAnchor(ResourceLocation id)
{
	/**
	 * Creates an attachment anchor from its namespaced identifier.
	 *
	 * @param id the namespaced resource or registration identifier
	 * @return the namespaced attachment anchor
	 */
	public static PAttachmentAnchor of(ResourceLocation id)
	{
		return new PAttachmentAnchor(id);
	}
	
	/**
	 * Creates an attachment anchor in the Minecraft namespace.
	 *
	 * @param path the path
	 * @return the attachment anchor in the Minecraft namespace
	 */
	public static PAttachmentAnchor minecraft(String path)
	{
		return of(ResourceLocation.withDefaultNamespace(path));
	}
}
