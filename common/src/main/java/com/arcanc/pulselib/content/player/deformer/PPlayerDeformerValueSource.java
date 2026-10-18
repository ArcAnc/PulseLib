/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.deformer;

import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import net.minecraft.world.entity.player.Player;

/**
 * Resolves deformation channel values from current player state.
 */
@FunctionalInterface
public interface PPlayerDeformerValueSource
{
	/**
	 * Samples a float deformation channel for the supplied player.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param reference the reference channel reference and its fallback value
	 * @return the current player-dependent deformation channel value
	 */
	float resolve(Player player, PChannelReference<Float> reference);
}
