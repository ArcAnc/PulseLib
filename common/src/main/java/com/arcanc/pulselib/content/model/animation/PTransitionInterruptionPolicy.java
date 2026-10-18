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
 * Interruption behavior for an in-progress pose-layer crossfade.
 */
public enum PTransitionInterruptionPolicy
{
	/**
	 * Captures the currently blended pose as the next transition source.
	 */
	FROM_CURRENT,
	/**
	 * Starts the next transition from its configured source layer.
	 */
	RESTART,
	/**
	 * Rejects a new transition while the current crossfade is active.
	 */
	COMPLETE_CURRENT
}
