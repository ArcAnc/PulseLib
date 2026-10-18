/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

/**
 * Transform-space rule used to combine a pose layer with a reference pose.
 */
public enum PPoseBlendMode
{
	/**
	 * Blends toward the layer transform.
	 */
	OVERRIDE,
	/**
	 * Adds the layer delta in local bone space.
	 */
	ADDITIVE_LOCAL,
	/**
	 * Adds the layer delta in model hierarchy space.
	 */
	ADDITIVE_MESH_SPACE,
	/**
	 * Multiplies the accumulated scale by the weighted layer scale.
	 */
	MULTIPLY_SCALE,
	/**
	 * Applies the difference between a layer and its reference pose.
	 */
	DIFFERENCE
}
