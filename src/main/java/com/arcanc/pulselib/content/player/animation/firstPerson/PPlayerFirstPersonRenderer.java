package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAttachmentLayer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;

/**
 * PulseLib's first-person extra-geometry coordinator.  It has no mixin entry
 * point: the following integration stage supplies the native 1.21.1 callback.
 */
public final class PPlayerFirstPersonRenderer
{
	private PPlayerFirstPersonRenderer() {}

	public static void renderExtras(LocalPlayer player, PFirstPersonRenderPresentation presentation,
	                                PoseStack poseStack, MultiBufferSource buffers, int packedLight, float partialTick)
	{
		PPlayerAutomaticMeshAttachments.renderFirstPerson(presentation.meshAttachments(), poseStack, packedLight);
		PPlayerAnimatedAttachments.renderFirstPerson(player, presentation, poseStack, buffers, packedLight, partialTick);
		renderPersistentAttachments(player, HumanoidArm.RIGHT, presentation.rightArm(), poseStack, packedLight, partialTick);
		renderPersistentAttachments(player, HumanoidArm.LEFT, presentation.leftArm(), poseStack, packedLight, partialTick);
	}

	private static void renderPersistentAttachments(LocalPlayer player, HumanoidArm arm, PFirstPersonArmPose pose,
	                                                PoseStack poseStack, int packedLight, float partialTick)
	{
		if (pose.mode() != PFirstPersonRenderMode.ANIMATED) return;
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        PlayerModel<?> model = playerRenderer.getModel();
		ModelPart armPart = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(PVanillaFirstPersonArmResolver.resolveArmOrigin(arm, pose.transform()).matrix());
			PHumanoidAttachmentLayer.renderFirstPersonArm(poseStack, packedLight, player, arm, armPart, partialTick);
		}
		finally { poseStack.popPose(); }
	}
}
