package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAttachmentLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;

/** PulseLib-owned extra first-person geometry; vanilla arms and items stay vanilla. */
public final class PPlayerFirstPersonRenderer
{
	private PPlayerFirstPersonRenderer()
	{
	}

	public static void renderExtras(LocalPlayer player,
	                                PPlayerFirstPersonPose pose,
	                                PoseStack poseStack,
	                                SubmitNodeCollector submitNodeCollector,
	                                int packedLight,
	                                float partialTick)
	{
		PPlayerAutomaticMeshAttachments.renderFirstPerson(
				pose.meshAttachments(), poseStack, submitNodeCollector, packedLight);
		PPlayerAnimatedAttachments.renderFirstPerson(
				player, pose, poseStack, submitNodeCollector, packedLight, partialTick);
		renderPersistentAttachments(player, pose, poseStack, packedLight, partialTick);
	}

	private static void renderPersistentAttachments(LocalPlayer player,
	                                                PPlayerFirstPersonPose pose,
	                                                PoseStack poseStack,
	                                                int packedLight,
	                                                float partialTick)
	{
		renderPersistentAttachments(player, HumanoidArm.RIGHT, pose.rightArm(), poseStack, packedLight, partialTick);
		renderPersistentAttachments(player, HumanoidArm.LEFT, pose.leftArm(), poseStack, packedLight, partialTick);
	}

	private static void renderPersistentAttachments(LocalPlayer player,
	                                                HumanoidArm arm,
	                                                PFirstPersonArmPose armPose,
	                                                PoseStack poseStack,
	                                                int packedLight,
	                                                float partialTick)
	{
		if (armPose.mode() != PFirstPersonRenderMode.ANIMATED)
			return;
		PlayerModel model = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player).getModel();
		ModelPart armPart = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(PVanillaFirstPersonArmResolver.resolveArmOrigin(arm, armPose.transform()).matrix());
			PHumanoidAttachmentLayer.renderFirstPersonArm(poseStack, packedLight, player, arm, armPart, partialTick);
		}
		finally
		{
			poseStack.popPose();
		}
	}
}
