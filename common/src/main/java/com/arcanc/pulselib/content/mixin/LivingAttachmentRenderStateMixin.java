/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.base.PLivingAttachmentRenderState;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

/** Stores an attachment snapshot without NeoForge render-data extensions. */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingAttachmentRenderStateMixin implements PLivingAttachmentRenderState
{
	@Unique
	private @Nullable List<PLivingAttachmentLayer.RenderEntry> pulselib$attachments;

	@Override
	public @Nullable List<PLivingAttachmentLayer.RenderEntry> pulselib$getAttachments()
	{
		return this.pulselib$attachments;
	}

	@Override
	public void pulselib$setAttachments(@Nullable List<PLivingAttachmentLayer.RenderEntry> entries)
	{
		this.pulselib$attachments = entries;
	}
}
