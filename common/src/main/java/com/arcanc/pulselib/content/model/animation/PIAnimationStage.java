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
 * Immutable definition of one stage in a raw animation.
 * <p>
 * A stage creates a new player for every controller playback. This keeps
 * per-playback state, such as a random selection, out of the shared animation
 * definition.
 */
@FunctionalInterface
public interface PIAnimationStage
{
	/**
	 * Creates the runtime player for this stage.
	 * @param context the stage context to use.
	 * @return the runtime player.
	 */
	PAnimationStagePlayer createPlayer(PAnimationStageContext context);

	/**
	 * Creates a copy with a different interpolation where supported.
	 * @param interpolation the interpolation to use.
	 * @return the copied stage.
	 */
	default PIAnimationStage withInterpolation(PInterpolationType interpolation)
	{
		throw new UnsupportedOperationException("This animation stage does not support interpolation");
	}

	/**
	 * Creates a copy with a different speed where supported.
	 * @param speed the speed to use.
	 * @return the copied stage.
	 */
	default PIAnimationStage withSpeed(float speed)
	{
		throw new UnsupportedOperationException("This animation stage does not support speed");
	}
}
