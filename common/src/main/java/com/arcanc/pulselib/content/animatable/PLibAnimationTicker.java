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
