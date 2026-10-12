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

@FunctionalInterface
public interface PPlayerAnimationWeight
{
	PPlayerAnimationWeight FULL = (player, partialTick) -> 1.0f;

	float weight(Player player, float partialTick);
}
