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

/**
 * Registry of locator callbacks invoked by animation events.
 */
public final class PAnimationEventCallbacks
{
	private static final Map<ResourceLocation, LocatorCallback> CALLBACKS = new ConcurrentHashMap<>();

	private PAnimationEventCallbacks() { }

	/**
	 * Associates a callback with its namespaced event identifier.
	 *
	 * @param id the namespaced resource or registration identifier
	 * @param callback the Mixin callback used to inspect or replace the intercepted result
	 */
	public static void register(ResourceLocation id, LocatorCallback callback)
	{
		if (CALLBACKS.putIfAbsent(id, callback) != null)
			throw new IllegalArgumentException("Animation locator callback already registered: " + id);
	}

	/**
	 * Removes the callback registered under the supplied identifier.
	 *
	 * @param id the namespaced resource or registration identifier
	 */
	public static void unregister(ResourceLocation id) { CALLBACKS.remove(id); }

	/**
	 * Invokes the registered callback with the resolved locator position when a callback exists.
	 *
	 * @param id the namespaced resource or registration identifier
	 * @param context the current event owner, controller, level and locator context
	 * @param locator the named model locator or bone used to resolve the event position
	 */
	public static void dispatch(ResourceLocation id, PAnimationEventContext context, String locator)
	{
		LocatorCallback callback = CALLBACKS.get(id);
		if (callback != null)
			callback.execute(context, context.position(locator));
	}

	/**
	 * Receives an animation event and the resolved position of its locator.
	 */
	@FunctionalInterface
	public interface LocatorCallback
	{
		/**
		 * Handles the animation event at the resolved locator position.
		 *
		 * @param context the current event owner, controller, level and locator context
		 * @param position the position
		 */
		void execute(PAnimationEventContext context, PAnimationEventContext.PAnimationEventDispatcherBridge.Position position);
	}
}
