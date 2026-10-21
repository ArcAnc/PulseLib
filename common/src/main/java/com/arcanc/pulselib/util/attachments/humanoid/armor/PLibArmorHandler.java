/**
 * @author ArcAnc
 * Created at: 05.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid.armor;

import com.arcanc.pulselib.content.mixin.LivingEntityRendererAccessor;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentLayer;
import com.arcanc.pulselib.content.renderer.base.PLivingAttachmentRenderState;
import com.arcanc.pulselib.content.renderer.base.PRenderState;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAttachmentLayer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Installs living, humanoid and animated-player attachment layers on compatible renderers.
 */
public final class PLibArmorHandler
{
	/**
	 * Adds the PulseLib attachment layers supported by the supplied entity renderer.
	 *
	 * @param renderer the renderer
	 */
	@SuppressWarnings ( {"rawtypes", "unchecked"})
	public static void addLayers(EntityRenderer<?, ?> renderer)
	{
		if (renderer instanceof AvatarRenderer player)
		{
			((LivingEntityRendererAccessor)player).pulselib$addLayer(new PHumanoidAttachmentLayer(player));
			((LivingEntityRendererAccessor)player).pulselib$addLayer(new PPlayerAnimatedAttachmentLayer(player));
		}
		else if (renderer instanceof LivingEntityRenderer living)
		{
			if (living.getModel() instanceof HumanoidModel)
			{
				((LivingEntityRendererAccessor)living).pulselib$addLayer(new PHumanoidAttachmentLayer(living));
			}
			else
			{
				((LivingEntityRendererAccessor)living).pulselib$addLayer(new PLivingAttachmentLayer(living));
			}
		}
	}
	/** Extracts the attachment snapshot for the vanilla render state. */
	public static void extractAttachmentRenderData(LivingEntity entity, LivingEntityRenderState state)
	{
		List<PLivingAttachmentLayer.RenderEntry> entries = PLivingAttachmentLayer.extractRenderEntries(entity, PRenderState.partialTick(state));
		((PLivingAttachmentRenderState)state).pulselib$setAttachments(entries.isEmpty() ? null : entries);
	}
}
