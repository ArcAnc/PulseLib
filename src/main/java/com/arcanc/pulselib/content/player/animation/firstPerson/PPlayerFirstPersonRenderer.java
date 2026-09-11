/**
 * @author ArcAnc
 * Created at: 06.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;


import com.arcanc.pulselib.content.mixin.ItemInHandRendererAccessor;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachments;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAutomaticMeshAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

public class PPlayerFirstPersonRenderer
{
	private static final int ARM_RENDER_ORDER = 10;

	private static void renderPersistentAttachments(LocalPlayer player, PPlayerFirstPersonPose pose, PoseStack poseStack, int packedLight, float partialTick)
	{
		PPlayerFirstPersonArmRenderer.renderAttachments(
				player, HumanoidArm.RIGHT, pose.rightArm().transform(), poseStack, packedLight, partialTick);
		PPlayerFirstPersonArmRenderer.renderAttachments(
				player, HumanoidArm.LEFT, pose.leftArm().transform(), poseStack, packedLight, partialTick);
	}

	private static void renderArms(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight)
	{
		OrderedSubmitNodeCollector armCollector = submitNodeCollector.order(ARM_RENDER_ORDER);

		if (player.isInvisible())
			return;
		PPlayerFirstPersonArmRenderer.render(player, HumanoidArm.RIGHT, pose.rightArm().transform(), poseStack, armCollector, packedLight);
		PPlayerFirstPersonArmRenderer.render(player, HumanoidArm.LEFT, pose.leftArm().transform(), poseStack, armCollector, packedLight);
	}

	private static void renderItem(LocalPlayer player,
	                               PPlayerFirstPersonPose pose,
	                               InteractionHand hand,
	                               HumanoidArm arm,
	                               PFirstPersonItemPose itemPose,
	                               PoseStack poseStack,
	                               SubmitNodeCollector submitNodeCollector,
	                               int packedLight)
	{
		var stack = player.getItemInHand(hand);
		if (stack.isEmpty() || pose.hidesItem(player, hand, stack))
			return;

		poseStack.pushPose();
		try
		{
			poseStack.mulPose(itemPose.transform());
			ItemInHandRenderer renderer = Minecraft.getInstance().gameRenderer.itemInHandRenderer;
			ItemDisplayContext displayContext = arm == HumanoidArm.RIGHT ?
					ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			if (stack.getItem() instanceof net.minecraft.world.item.MapItem)
				((ItemInHandRendererAccessor)renderer).pulselib$renderMap(poseStack, submitNodeCollector, packedLight, stack);
			else
				renderer.renderItem(player, stack, displayContext, poseStack, submitNodeCollector, packedLight);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	private static PFirstPersonItemPose itemPose(PPlayerFirstPersonPose pose, HumanoidArm arm)
	{
		return arm == HumanoidArm.RIGHT ? pose.rightItem() : pose.leftItem();
	}

	public static void render(
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
			float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
			float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
			poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1f));
			poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1f));

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

			HumanoidArm mainArm = player.getMainArm();
			renderItem(player, pose, InteractionHand.MAIN_HAND, mainArm, itemPose(pose, mainArm), poseStack, submitNodeCollector, packedLight);
			HumanoidArm offArm = mainArm.getOpposite();
			renderItem(player, pose, InteractionHand.OFF_HAND, offArm, itemPose(pose, offArm), poseStack, submitNodeCollector, packedLight);

			renderPersistentAttachments(
					player,
					pose,
					poseStack,
					packedLight,
					partialTick);

			Minecraft minecraft = Minecraft.getInstance();
			minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
			minecraft.renderBuffers().bufferSource().endBatch();

		}
		finally
		{
			poseStack.popPose();
		}
	}
}
