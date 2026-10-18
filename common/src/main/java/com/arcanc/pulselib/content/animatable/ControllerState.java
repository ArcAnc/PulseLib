/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

/**
 * Playback state requested by an animation controller state handler.
 */
public enum ControllerState
{
	/**
	 * Advances and samples the active animation.
	 */
	PLAY,
	/**
	 * Keeps the active playback position without advancing it.
	 */
	PAUSE,
	/**
	 * Stops playback until another animation is selected.
	 */
	STOP;
}
