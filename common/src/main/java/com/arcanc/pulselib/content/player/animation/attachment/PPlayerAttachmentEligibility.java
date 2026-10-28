/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.attachment;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchorPose;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.ApiStatus;

/** A callback can choose any material during submission; extraction must retain its owner's native targets beforehand. */
@ApiStatus.Internal
public final class PPlayerAttachmentEligibility
{
	private PPlayerAttachmentEligibility()
	{
	}

	public static boolean hasRenderer(AbstractClientPlayer player, float partialTick)
	{
		for (PPlayerAnimationAnchorPose pose : PPlayerAnimations.animationAnchorPoses(player, partialTick))
			if (PPlayerAnimatedAttachments.hasRenderer(pose.anchor()))
				return true;
		return false;
	}
}
