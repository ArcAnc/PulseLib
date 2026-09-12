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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

public class PPlayerFirstPersonRenderer
{
	private static final int ARM_RENDER_ORDER = 10;

	private static void renderPersistentAttachments(LocalPlayer player, PPlayerFirstPersonPose pose, PoseStack poseStack, int packedLight, float partialTick)
	{
		renderPersistentAttachments(player, HumanoidArm.RIGHT, pose.rightArm(), poseStack, packedLight, partialTick);
		renderPersistentAttachments(player, HumanoidArm.LEFT, pose.leftArm(), poseStack, packedLight, partialTick);
	}

	private static void renderPersistentAttachments(
			LocalPlayer player,
			HumanoidArm arm,
			PFirstPersonArmPose armPose,
			PoseStack poseStack,
			int packedLight,
			float partialTick)
	{
		if (armPose.mode() == PFirstPersonRenderMode.ANIMATED)
			PPlayerFirstPersonArmRenderer.renderAttachments(
					player, arm, armPose.transform(), poseStack, packedLight, partialTick);
	}

	private static void renderArms(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight,
			float partialTick)
	{
		OrderedSubmitNodeCollector armCollector = submitNodeCollector.order(ARM_RENDER_ORDER);

		if (player.isInvisible())
			return;
		InteractionHand rightHand = player.getMainArm() == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		renderArm(player, pose, rightHand, HumanoidArm.RIGHT, pose.rightArm(), poseStack, armCollector, submitNodeCollector, packedLight, partialTick);
		renderArm(player, pose, rightHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
				HumanoidArm.LEFT, pose.leftArm(), poseStack, armCollector, submitNodeCollector, packedLight, partialTick);
	}

	private static void renderArm(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			InteractionHand hand,
			HumanoidArm arm,
			PFirstPersonArmPose armPose,
			PoseStack poseStack,
			OrderedSubmitNodeCollector armCollector,
			SubmitNodeCollector submitNodeCollector,
			int packedLight,
			float partialTick)
	{
		if (usesCompleteVanillaHandPath(player, pose, hand, arm))
			return;

		switch (armPose.mode())
		{
			case HIDDEN ->
			{
			}
			case ANIMATED -> PPlayerFirstPersonArmRenderer.render(
					player, arm, armPose.transform(), poseStack, armCollector, packedLight);
			case VANILLA -> PPlayerFirstPersonVanillaRenderer.renderVanillaArm(
					player, hand, arm, poseStack, submitNodeCollector, packedLight, partialTick);
		}
	}

	private static void renderItem(LocalPlayer player,
	                               PPlayerFirstPersonPose pose,
	                               InteractionHand hand,
	                               HumanoidArm arm,
	                               PFirstPersonItemPose itemPose,
	                               PoseStack poseStack,
	                               SubmitNodeCollector submitNodeCollector,
	                               int packedLight,
	                               float partialTick)
	{
		if (usesCompleteVanillaHandPath(player, pose, hand, arm))
			return;

		var stack = player.getItemInHand(hand);
		switch (itemPose.mode())
		{
			case HIDDEN ->
			{
			}
			case VANILLA ->
			{
				if (!pose.hidesItem(player, hand, stack))
					PPlayerFirstPersonVanillaRenderer.renderVanillaItem(
							player, hand, arm, poseStack, submitNodeCollector, packedLight, partialTick);
			}
			case ANIMATED ->
			{
				if (!stack.isEmpty() && !pose.hidesItem(player, hand, stack))
					renderAnimatedItem(player, arm, itemPose, stack, poseStack, submitNodeCollector, packedLight);
			}
		}
	}

	private static void renderAnimatedItem(
			LocalPlayer player,
			HumanoidArm arm,
			PFirstPersonItemPose itemPose,
			net.minecraft.world.item.ItemStack stack,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight)
	{
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(itemPose.transform().matrix());
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

	private static void renderCompleteVanillaHands(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight,
			float partialTick)
	{
		HumanoidArm mainArm = player.getMainArm();
		if (usesCompleteVanillaHandPath(player, pose, InteractionHand.MAIN_HAND, mainArm))
			PPlayerFirstPersonVanillaRenderer.renderVanillaHand(
					player, InteractionHand.MAIN_HAND, poseStack, submitNodeCollector, packedLight, partialTick);

		HumanoidArm offArm = mainArm.getOpposite();
		if (usesCompleteVanillaHandPath(player, pose, InteractionHand.OFF_HAND, offArm))
			PPlayerFirstPersonVanillaRenderer.renderVanillaHand(
					player, InteractionHand.OFF_HAND, poseStack, submitNodeCollector, packedLight, partialTick);
	}

	private static boolean usesCompleteVanillaHandPath(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			InteractionHand hand,
			HumanoidArm arm)
	{
		if (!hasVanillaHandChannels(player, pose, hand, arm))
			return false;

		if (hand != InteractionHand.MAIN_HAND || !PPlayerFirstPersonVanillaRenderer.rendersTwoHandedMap())
			return true;

		return hasVanillaHandChannels(player, pose, InteractionHand.OFF_HAND, arm.getOpposite());
	}

	private static boolean hasVanillaHandChannels(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			InteractionHand hand,
			HumanoidArm arm)
	{
		return (arm == HumanoidArm.RIGHT ? pose.rightArm() : pose.leftArm()).mode() == PFirstPersonRenderMode.VANILLA &&
				itemPose(pose, arm).mode() == PFirstPersonRenderMode.VANILLA &&
				!pose.hidesItem(player, hand, player.getItemInHand(hand));
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

			renderCompleteVanillaHands(
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
					packedLight,
					partialTick);

			HumanoidArm mainArm = player.getMainArm();
			renderItem(player, pose, InteractionHand.MAIN_HAND, mainArm, itemPose(pose, mainArm), poseStack, submitNodeCollector, packedLight, partialTick);
			HumanoidArm offArm = mainArm.getOpposite();
			renderItem(player, pose, InteractionHand.OFF_HAND, offArm, itemPose(pose, offArm), poseStack, submitNodeCollector, packedLight, partialTick);

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
