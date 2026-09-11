/**
 * @author ArcAnc
 * Created at: 05.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Objects;

public record PPlayerFirstPersonPose(
		PFirstPersonArmPose rightArm,
		PFirstPersonArmPose leftArm,
		PFirstPersonItemPose rightItem,
		PFirstPersonItemPose leftItem,
		List<PPlayerFirstPersonAnchorPose> animationAnchors,
		List<PPlayerFirstPersonMeshAttachmentPose> meshAttachments)
{
	public PPlayerFirstPersonPose
	{
		rightArm = Objects.requireNonNull(rightArm);
		leftArm = Objects.requireNonNull(leftArm);
		rightItem = Objects.requireNonNull(rightItem);
		leftItem = Objects.requireNonNull(leftItem);
		animationAnchors = List.copyOf(animationAnchors);
		meshAttachments = List.copyOf(meshAttachments);
	}

	public boolean hidesItem(LocalPlayer player, InteractionHand hand, ItemStack stack)
	{
		PFirstPersonItemPose itemPose = hand == InteractionHand.MAIN_HAND ?
				(player.getMainArm() == HumanoidArm.RIGHT ? this.rightItem : this.leftItem) :
				(player.getMainArm() == HumanoidArm.RIGHT ? this.leftItem : this.rightItem);
		return itemPose.renderPolicy().hide(player, hand, stack);
	}
}
