/**
 * @author ArcAnc
 * Created at: 11.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

/**
 * Choice between vanilla, animation-driven and hidden first-person geometry.
 */
public enum PFirstPersonRenderMode
{
	/**
	 * Renders using the original vanilla hand-pass transform.
	 */
	VANILLA,
	/**
	 * Renders using the sampled animation transform.
	 */
	ANIMATED,
	/**
	 * Suppresses the selected first-person geometry.
	 */
	HIDDEN
}
