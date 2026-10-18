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
 * Action requested by a stage player after its playback completes.
 */
public enum PAnimationStageTransition
{
	/**
	 * Continues with the next stage in the raw animation sequence.
	 */
	NEXT_STAGE,
	/**
	 * Resolves another playback from the same stage player.
	 */
	REPEAT_PLAYBACK
}
