package com.arcanc.pulselib.util.attachments.humanoid.armor;

import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentLayer;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAttachmentLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class PLibArmorHandler
{
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void addLayers(EntityRenderer<?> renderer)
	{
		if (renderer instanceof PlayerRenderer player)
		{
			((com.arcanc.pulselib.content.mixin.LivingEntityRendererAccessor) player).pulselib$addLayer(new PHumanoidAttachmentLayer(player));
			((com.arcanc.pulselib.content.mixin.LivingEntityRendererAccessor) player).pulselib$addLayer(new PPlayerAnimatedAttachmentLayer(player));
		}
		else if (renderer instanceof LivingEntityRenderer living)
		{
			if (living.getModel() instanceof HumanoidModel)
				((com.arcanc.pulselib.content.mixin.LivingEntityRendererAccessor) living).pulselib$addLayer(new PHumanoidAttachmentLayer(living));
			else
				((com.arcanc.pulselib.content.mixin.LivingEntityRendererAccessor) living).pulselib$addLayer(new PLivingAttachmentLayer(living));
		}
	}
}
