/**
 * @author ArcAnc
 * Created at: 24.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


/**
 * Per-controller runtime state for an animation stage.
 */
public interface PAnimationStagePlayer
{
	/**
	 * Returns the playback command currently produced by this stage.
	 * @return the current playback command.
	 */
	PAnimationStagePlayback currentPlayback();

	/**
	 * Advances the stage after its current playback completes.
	 * @return the action for the controller to take next.
	 */
	PAnimationStageTransition complete();
}
