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
 * Logical-side policy used to decide whether an animation event should execute.
 */
public enum PEventSide
{
	/**
	 * Executes in client levels.
	 */
	CLIENT,
	/**
	 * Executes in server levels.
	 */
	SERVER,
	/**
	 * Executes on both logical sides.
	 */
	BOTH,
	/**
	 * Executes only as a client presentation effect.
	 */
	PRESENTATION_ONLY
}
