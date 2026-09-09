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
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class PPlayerFirstPersonRenderer
{
	private static final int ARM_RENDER_ORDER = 10;
	
	private static void renderItems(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight)
	{
		ItemInHandRenderer renderer = PLibRenderHelper.mc().gameRenderer.itemInHandRenderer;
		boolean mainIsRight = player.getMainArm() == HumanoidArm.RIGHT;
		renderItem(renderer, player, player.getMainHandItem(), InteractionHand.MAIN_HAND, mainIsRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
				mainIsRight ? pose.rightItem() : pose.leftItem(), poseStack, submitNodeCollector, packedLight);
		renderItem(renderer, player, player.getOffhandItem(), InteractionHand.OFF_HAND, mainIsRight ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
				mainIsRight ? pose.leftItem() : pose.rightItem(), poseStack, submitNodeCollector, packedLight);
	}

	private static void renderItem(ItemInHandRenderer renderer, LocalPlayer player, ItemStack stack, InteractionHand hand, ItemDisplayContext context, Matrix4f transform, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight)
	{
		if (transform == null || stack.isEmpty())
			return;
		poseStack.pushPose();
		try
		{
			poseStack.mulPose(transform);
			poseStack.mulPose(PPlayerFirstPersonItemTransform.correction(stack, hand));
			renderer.renderItem(player, stack, context, poseStack, submitNodeCollector, packedLight);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	private static void renderPersistentAttachments(LocalPlayer player, PPlayerFirstPersonPose pose, PoseStack poseStack, int packedLight)
	{
		if (pose.rightArm() != null)
			PPlayerFirstPersonArmRenderer.renderAttachments(player, HumanoidArm.RIGHT, pose.rightArm(), poseStack, packedLight, partialTick());
		if (pose.leftArm() != null)
			PPlayerFirstPersonArmRenderer.renderAttachments(player, HumanoidArm.LEFT, pose.leftArm(), poseStack, packedLight, partialTick());
	}

	private static float partialTick()
	{
		return PLibRenderHelper.mc().isPaused() ? 0.0f : PLibRenderHelper.mc().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	private static void renderArms(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight)
	{
		OrderedSubmitNodeCollector armCollector = submitNodeCollector.order(ARM_RENDER_ORDER);
		
		Matrix4f rightArm = pose.rightArm();
		
		if (rightArm != null)
		{
			PPlayerFirstPersonArmRenderer.render(
					player,
					HumanoidArm.RIGHT,
					rightArm,
					poseStack,
					armCollector,
					packedLight);
		}
		
		Matrix4f leftArm = pose.leftArm();
		
		if (leftArm != null)
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
	
	public static void render(
			LocalPlayer player,
			PPlayerFirstPersonPose pose,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight)
	{
		poseStack.pushPose();
		try
		{
			Matrix4f firstPersonPose = new Matrix4f(poseStack.last().pose());
			poseStack.mulPose(new Matrix4f(firstPersonPose).invert());

			renderItems(
				player,
				pose,
				poseStack,
				submitNodeCollector,
				packedLight);

			poseStack.pushPose();
			try
			{
				poseStack.mulPose(firstPersonPose);
				PPlayerAutomaticMeshAttachments.renderFirstPerson(
						pose.meshAttachments(),
						poseStack,
						submitNodeCollector,
						packedLight);
			}
			finally
			{
				poseStack.popPose();
			}

			PPlayerAnimatedAttachments.renderFirstPerson(
					player,
					pose,
					poseStack,
					submitNodeCollector,
					packedLight,
					partialTick());
		
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
				packedLight);

		}
		finally
		{
			poseStack.popPose();
		}
	}
}
