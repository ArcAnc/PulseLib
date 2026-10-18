/**
 * @author ArcAnc
 * Created at: 09.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;

/**
 * Resolves mesh deformation channel values from a player animation instance.
 */
@FunctionalInterface
public interface PPlayerAnimationDeformerValueSource
{
	/**
	 * Samples the referenced deformation channel using current player animation state.
	 *
	 * @param context the current player animation state used to sample deformation channels
	 * @param reference the reference channel reference and its fallback value
	 * @return the current deformation channel value
	 */
	float resolve(PPlayerAnimationDeformerContext context, PChannelReference<Float> reference);
}
