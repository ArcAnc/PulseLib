/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


/**
 * Completion policy used when an animation clip reaches its playback boundary.
 */
public enum PAnimationType
{
	/**
	 * Plays the clip once and allows playback to complete.
	 */
	PLAY_ONCE,
	/**
	 * Stops advancing at the clip boundary while retaining its final pose.
	 */
	HOLD_LAST_FRAME,
	/**
	 * Wraps playback around the clip length to repeat the animation.
	 */
	CYCLE;
}
