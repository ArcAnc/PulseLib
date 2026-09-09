/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public record PPlayerAnimationAnchorPose(
		Identifier animation,
		PPlayerAnimationAnchor anchor,
		Matrix4f transform,
		float weight)
{
	public PPlayerAnimationAnchorPose
	{
		transform = new Matrix4f(transform);
	}
}
