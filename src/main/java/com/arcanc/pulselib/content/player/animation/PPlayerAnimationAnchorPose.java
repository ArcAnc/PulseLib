/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.PTransform;
import net.minecraft.resources.Identifier;

public record PPlayerAnimationAnchorPose(
		Identifier animation,
		PPlayerAnimationAnchor anchor,
		PTransform transform,
		float weight)
{
}
