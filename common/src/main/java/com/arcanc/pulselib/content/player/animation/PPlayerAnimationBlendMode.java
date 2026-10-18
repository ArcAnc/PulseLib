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

/**
 * Player animation blend policy mapped to the corresponding pose mixer mode.
 */
public enum PPlayerAnimationBlendMode
{
	/**
	 * Blends toward the layer transform.
	 */
	OVERRIDE(PPoseBlendMode.OVERRIDE),
	/**
	 * Adds the layer delta in local bone space.
	 */
	ADDITIVE_LOCAL(PPoseBlendMode.ADDITIVE_LOCAL),
	/**
	 * Adds the layer delta in model hierarchy space.
	 */
	ADDITIVE_MESH_SPACE(PPoseBlendMode.ADDITIVE_MESH_SPACE),
	/**
	 * Multiplies the accumulated scale by the weighted layer scale.
	 */
	MULTIPLY_SCALE(PPoseBlendMode.MULTIPLY_SCALE),
	/**
	 * Applies the difference between a layer and its reference pose.
	 */
	DIFFERENCE(PPoseBlendMode.DIFFERENCE);

	private final PPoseBlendMode poseBlendMode;

	/**
	 * Creates a player animation blend mode from the supplied configuration.
	 *
	 * @param poseBlendMode the pose blend mode
	 */
	PPlayerAnimationBlendMode(PPoseBlendMode poseBlendMode)
	{
		this.poseBlendMode = poseBlendMode;
	}

	/**
	 * Returns the pose blend mode.
	 *
	 * @return the pose blend mode
	 */
	public PPoseBlendMode poseBlendMode()
	{
		return this.poseBlendMode;
	}
}
