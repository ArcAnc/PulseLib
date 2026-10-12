/**
 * @author ArcAnc
 * Created at: 04.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid.armor;

import com.arcanc.pulselib.content.mixin.LivingEntityRendererAccessor;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentLayer;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAttachmentLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class PLibArmorHandler
{
	@SuppressWarnings ( {"rawtypes", "unchecked"})
	public static void addLayers(EntityRenderer<?> renderer)
	{
		if (renderer instanceof PlayerRenderer player)
		{
			((LivingEntityRendererAccessor)player).pulselib$addLayer(new PHumanoidAttachmentLayer(player));
			((LivingEntityRendererAccessor)player).pulselib$addLayer(new PPlayerAnimatedAttachmentLayer(player));
		}
		else if (renderer instanceof LivingEntityRenderer living)
		{
			if (living.getModel() instanceof HumanoidModel)
				((LivingEntityRendererAccessor)living).pulselib$addLayer(new PHumanoidAttachmentLayer(living));
			else
				((LivingEntityRendererAccessor)living).pulselib$addLayer(new PLivingAttachmentLayer(living));
		}
	}
}
