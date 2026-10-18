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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Selects the player model parts eligible to receive an animation contribution.
 */
@FunctionalInterface
public interface PPlayerAnimationMask
{
	/**
	 * Tests whether the supplied player part is included for the current frame.
	 *
	 * @param player the player whose animation or presentation is being processed
	 * @param part the player model part receiving the contribution
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return whether the supplied player part is included for the current frame
	 */
	boolean contains(Player player, PPlayerPart part, float partialTick);

	/**
	 * Creates a mask that includes the supplied player parts.
	 *
	 * @param parts the player parts included by the mask
	 * @return a mask containing the supplied player parts
	 */
	static PPlayerAnimationMask of(PPlayerPart... parts)
	{
		Objects.requireNonNull(parts);
		EnumSet<PPlayerPart> allowed = EnumSet.noneOf(PPlayerPart.class);
		Arrays.stream(parts).forEach(part -> allowed.add(Objects.requireNonNull(part)));
		return of(allowed);
	}

	/**
	 * Creates a mask that includes the supplied player parts.
	 *
	 * @param parts the player parts included by the mask
	 * @return a mask containing the supplied player parts
	 */
	static PPlayerAnimationMask of(Set<PPlayerPart> parts)
	{
		Set<PPlayerPart> allowed = Set.copyOf(parts);
		return (player, part, partialTick) -> allowed.contains(part);
	}
}
