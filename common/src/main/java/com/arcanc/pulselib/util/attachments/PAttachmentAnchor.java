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

public record PAttachmentAnchor(ResourceLocation id)
{
	public static PAttachmentAnchor of(ResourceLocation id)
	{
		return new PAttachmentAnchor(id);
	}
	
	public static PAttachmentAnchor minecraft(String path)
	{
		return of(ResourceLocation.withDefaultNamespace(path));
	}
}
