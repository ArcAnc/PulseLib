/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolved animated action anchor in first-person player presentation.
 *
 * @param animation the namespaced resource or registry identifier
 * @param anchor the anchor
 * @param transform the transform
 * @param weight the contribution weight
 */
public record PPlayerFirstPersonAnchorPose(
		ResourceLocation animation,
		PPlayerAnimationAnchor anchor,
		PTransform transform,
		float weight)
{
}
