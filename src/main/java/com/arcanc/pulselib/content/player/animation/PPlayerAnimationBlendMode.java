/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.PPoseBlendMode;

public enum PPlayerAnimationBlendMode
{
	OVERRIDE(PPoseBlendMode.OVERRIDE),
	ADDITIVE_LOCAL(PPoseBlendMode.ADDITIVE_LOCAL),
	ADDITIVE_MESH_SPACE(PPoseBlendMode.ADDITIVE_MESH_SPACE),
	MULTIPLY_SCALE(PPoseBlendMode.MULTIPLY_SCALE),
	DIFFERENCE(PPoseBlendMode.DIFFERENCE);

	private final PPoseBlendMode poseBlendMode;

	/**
	 * Creates an instance of the enclosing type.
	 * @param poseBlendMode the pose blend mode to use.
	 */
	PPlayerAnimationBlendMode(PPoseBlendMode poseBlendMode)
	{
		this.poseBlendMode = poseBlendMode;
	}

	/**
	 * Performs the pose blend mode operation.
	 * @return the value produced by this operation.
	 */
	public PPoseBlendMode poseBlendMode()
	{
		return this.poseBlendMode;
	}
}
