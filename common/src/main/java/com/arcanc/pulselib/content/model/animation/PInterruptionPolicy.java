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
 * Policy controlling how an active graph transition responds to another eligible transition.
 */
public enum PInterruptionPolicy
{
	/**
	 * Starts the new transition from the currently blended pose.
	 */
	FROM_CURRENT,
	/**
	 * Restarts transition playback using the runtime restart policy.
	 */
	RESTART,
	/**
	 * Defers interruption until the current transition has finished.
	 */
	COMPLETE_CURRENT
}
