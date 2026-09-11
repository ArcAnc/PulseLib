/**
 * @author ArcAnc
 * Created at: 05.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;


import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Objects;

public record PPlayerFirstPersonPose(
		PFirstPersonArmPose rightArm,
		PFirstPersonArmPose leftArm,
		PFirstPersonItemPose rightItem,
		PFirstPersonItemPose leftItem,
		PPlayerAnimationDefinition.FPItemHider itemHider,
		List<PPlayerFirstPersonAnchorPose> animationAnchors,
		List<PPlayerFirstPersonMeshAttachmentPose> meshAttachments)
{
	public PPlayerFirstPersonPose
	{
		rightArm = Objects.requireNonNull(rightArm);
		leftArm = Objects.requireNonNull(leftArm);
		rightItem = Objects.requireNonNull(rightItem);
		leftItem = Objects.requireNonNull(leftItem);
		itemHider = Objects.requireNonNull(itemHider);
		animationAnchors = List.copyOf(animationAnchors);
		meshAttachments = List.copyOf(meshAttachments);
	}

	public boolean hidesItem(LocalPlayer player, InteractionHand hand, ItemStack stack)
	{
		return this.itemHider.hide(player, hand, stack);
	}
}
