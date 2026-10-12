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

@FunctionalInterface
public interface PPlayerAnimationMask
{
	boolean contains(Player player, PPlayerPart part, float partialTick);

	static PPlayerAnimationMask of(PPlayerPart... parts)
	{
		Objects.requireNonNull(parts);
		EnumSet<PPlayerPart> allowed = EnumSet.noneOf(PPlayerPart.class);
		Arrays.stream(parts).forEach(part -> allowed.add(Objects.requireNonNull(part)));
		return of(allowed);
	}

	static PPlayerAnimationMask of(Set<PPlayerPart> parts)
	{
		Set<PPlayerPart> allowed = Set.copyOf(parts);
		return (player, part, partialTick) -> allowed.contains(part);
	}
}
