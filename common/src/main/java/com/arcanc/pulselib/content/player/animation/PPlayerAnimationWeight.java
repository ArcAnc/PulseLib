/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import net.minecraft.world.entity.player.Player;

/**
 * Samples a player animation contribution weight for the current render frame.
 */
@FunctionalInterface
public interface PPlayerAnimationWeight
{
	/**
	 * Weight provider that always returns full contribution.
	 */
	PPlayerAnimationWeight FULL = (player, partialTick) -> 1.0f;

	/**
	 * Returns the contribution weight for the supplied player and interpolation fraction.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the contribution weight for the supplied player and interpolation fraction
	 */
	float weight(Player player, float partialTick);
}
