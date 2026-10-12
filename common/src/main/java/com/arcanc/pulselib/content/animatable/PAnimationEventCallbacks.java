/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;

import com.arcanc.pulselib.content.model.animation.PAnimationEventContext;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PAnimationEventCallbacks
{
	private static final Map<ResourceLocation, LocatorCallback> CALLBACKS = new ConcurrentHashMap<>();

	private PAnimationEventCallbacks() { }

	public static void register(ResourceLocation id, LocatorCallback callback)
	{
		if (CALLBACKS.putIfAbsent(id, callback) != null)
			throw new IllegalArgumentException("Animation locator callback already registered: " + id);
	}

	public static void unregister(ResourceLocation id) { CALLBACKS.remove(id); }

	public static void dispatch(ResourceLocation id, PAnimationEventContext context, String locator)
	{
		LocatorCallback callback = CALLBACKS.get(id);
		if (callback != null)
			callback.execute(context, context.position(locator));
	}

	@FunctionalInterface
	public interface LocatorCallback
	{
		void execute(PAnimationEventContext context, PAnimationEventContext.PAnimationEventDispatcherBridge.Position position);
	}
}
