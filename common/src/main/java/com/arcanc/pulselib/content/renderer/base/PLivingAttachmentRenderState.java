/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.base;

import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Loader-independent attachment snapshot carried by a vanilla living render state. */
public interface PLivingAttachmentRenderState
{
	@Nullable List<PLivingAttachmentLayer.RenderEntry> pulselib$getAttachments();

	void pulselib$setAttachments(@Nullable List<PLivingAttachmentLayer.RenderEntry> entries);
}
