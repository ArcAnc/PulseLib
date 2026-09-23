/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

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
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PPlayerFirstPersonRenderer()
	{
	}

	/**
	 * Renders the extras.
	 * @param player the player to use.
	 * @param pose the pose to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
	public static void renderExtras(LocalPlayer player,
	                                PFirstPersonRenderPresentation pose,
	                                PoseStack poseStack,
	                                SubmitNodeCollector submitNodeCollector,
	                                int packedLight,
	                                float partialTick)
	{
		PPlayerAutomaticMeshAttachments.renderFirstPerson(
				pose.meshAttachments(), poseStack, packedLight);
		PPlayerAnimatedAttachments.renderFirstPerson(
				player, pose, poseStack, submitNodeCollector, packedLight, partialTick);
		renderPersistentAttachments(player, pose, poseStack, packedLight, partialTick);
	}

	/**
	 * Renders the persistent attachments.
	 * @param player the player to use.
	 * @param pose the pose to use.
	 * @param poseStack the pose stack to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
	private static void renderPersistentAttachments(LocalPlayer player,
	                                                PFirstPersonRenderPresentation pose,
	                                                PoseStack poseStack,
	                                                int packedLight,
	                                                float partialTick)
	{
		renderPersistentAttachments(player, HumanoidArm.RIGHT, pose.rightArm(), poseStack, packedLight, partialTick);
		renderPersistentAttachments(player, HumanoidArm.LEFT, pose.leftArm(), poseStack, packedLight, partialTick);
	}

	/**
	 * Renders the persistent attachments.
	 * @param player the player to use.
	 * @param arm the arm to use.
	 * @param armPose the arm pose to use.
	 * @param poseStack the pose stack to use.
	 * @param packedLight the packed light to use.
	 * @param partialTick the partial tick to use.
	 */
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
