/**
 * @author ArcAnc
 * Created at: 06.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;


import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;

public class PPlayerFirstPersonRenderer
{
	private static final int ARM_RENDER_ORDER = 10;
	
	private static void renderPersistentAttachments(LocalPlayer player, PPlayerFirstPersonPose pose, PoseStack poseStack, int packedLight, float partialTick)
	{
		if (pose.rightArm().mode() == PFirstPersonRenderMode.ANIMATED)
			PPlayerFirstPersonArmRenderer.renderAttachments(
					player,
					HumanoidArm.RIGHT,
					pose.rightArm().transform(),
					poseStack,
					packedLight,
					partialTick);
		if (pose.leftArm().mode() == PFirstPersonRenderMode.ANIMATED)
			PPlayerFirstPersonArmRenderer.renderAttachments(
					player,
					HumanoidArm.LEFT,
					pose.leftArm().transform(),
					poseStack,
					packedLight,
					partialTick);
	}

	private static void renderArms(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight)
	{
		OrderedSubmitNodeCollector armCollector = submitNodeCollector.order(ARM_RENDER_ORDER);
		
		Matrix4f rightArm = pose.rightArm().transform();
		
		if (pose.rightArm().mode() == PFirstPersonRenderMode.ANIMATED)
		{
			PPlayerFirstPersonArmRenderer.render(
					player,
					HumanoidArm.RIGHT,
					rightArm,
					poseStack,
					armCollector,
					packedLight);
		}
		
		Matrix4f leftArm = pose.leftArm().transform();
		
		if (pose.leftArm().mode() == PFirstPersonRenderMode.ANIMATED)
		{
			PPlayerFirstPersonArmRenderer.render(
					player,
					HumanoidArm.LEFT,
					leftArm,
					poseStack,
					armCollector,
					packedLight);
		}
	}
	
	public static void renderAdditional(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight,
			float partialTick)
	{
		poseStack.pushPose();
		try
		{
			PPlayerAutomaticMeshAttachments.renderFirstPerson(
					pose.meshAttachments(),
					poseStack,
					submitNodeCollector,
					packedLight);

			PPlayerAnimatedAttachments.renderFirstPerson(
					player,
					pose,
					poseStack,
					submitNodeCollector,
					packedLight,
					partialTick);
		
			renderArms(
				player,
				pose,
				poseStack,
				submitNodeCollector,
				packedLight);

			renderPersistentAttachments(
				player,
				pose,
				poseStack,
				packedLight,
				partialTick);

		}
		finally
		{
			poseStack.popPose();
		}
	}
}
