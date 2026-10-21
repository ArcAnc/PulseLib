/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;

import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.util.PResourceCache;
import com.mojang.blaze3d.systems.RenderSystem;

/** Physical-client lifecycle operations shared by loader adapters. */
public final class PulseRuntime
{
	private PulseRuntime()
	{
	}

	public static void beforeResourceReload()
	{
		PResourceCache.reloadRegistrations();
	}

	public static void disconnected()
	{
		RenderSystem.assertOnRenderThread();
		PRenderQueue.cleanUp();
		SingletonAnimationManager.cleanUp();
		InstanceAnimationManager.cleanUp();
		PPlayerAnimations.cleanUp();
	}
}
