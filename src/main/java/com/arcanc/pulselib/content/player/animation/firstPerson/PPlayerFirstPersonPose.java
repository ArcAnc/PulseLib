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
import org.joml.Matrix4f;

import java.util.List;

public record PPlayerFirstPersonPose(
		Matrix4f rightArm,
		Matrix4f leftArm,
		Matrix4f rightItem,
		Matrix4f leftItem,
		PPlayerAnimationDefinition.FPItemHider itemHider,
		List<PPlayerFirstPersonAnchorPose> animationAnchors,
		List<PPlayerFirstPersonMeshAttachmentPose> meshAttachments)
{
	public PPlayerFirstPersonPose
	{
		animationAnchors = List.copyOf(animationAnchors);
		meshAttachments = List.copyOf(meshAttachments);
	}

	public boolean hidesItem(LocalPlayer player, InteractionHand hand, ItemStack stack)
	{
		return this.itemHider.hide(player, hand, stack);
	}
}
