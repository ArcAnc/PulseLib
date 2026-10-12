/**
 * @author ArcAnc
 * Created at: 13.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import net.minecraft.client.multiplayer.ClientLevel;

public final class PLibAnimationTicker
{
	public static void clientTick(ClientLevel level)
	{
		SingletonAnimationManager.tickAll();
		InstanceAnimationManager.tickAll();
		PAnimationCameraShake.tick();
		PPlayerAnimations.tick(level);
	}

	public static void afterClientTick()
	{
		SingletonAnimationManager.removeUnused();
		InstanceAnimationManager.removeUnused();
	}
}
