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
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides support for animation event callbacks.
 */
public final class PAnimationEventCallbacks
{
	private static final Map<Identifier, LocatorCallback> CALLBACKS = new ConcurrentHashMap<>();

	/**
	 * Creates an instance of the enclosing type.
	 */
	private PAnimationEventCallbacks() { }

	/**
	 * Performs the register operation.
	 * @param id the id to use.
	 * @param callback the callback to use.
	 */
	public static void register(Identifier id, LocatorCallback callback)
	{
		if (CALLBACKS.putIfAbsent(id, callback) != null)
			throw new IllegalArgumentException("Animation locator callback already registered: " + id);
	}

	/**
	 * Performs the unregister operation.
	 * @param id the id to use.
	 */
	public static void unregister(Identifier id) { CALLBACKS.remove(id); }

	/**
	 * Performs the dispatch operation.
	 * @param id the id to use.
	 * @param context the context to use.
	 * @param locator the locator to use.
	 */
	public static void dispatch(Identifier id, PAnimationEventContext context, String locator)
	{
		LocatorCallback callback = CALLBACKS.get(id);
		if (callback != null)
			callback.execute(context, context.position(locator));
	}

	@FunctionalInterface
/**
 * Defines the contract for locator callback.
 */
	public interface LocatorCallback
	{
		/**
		 * Performs the execute operation.
		 * @param context the context to use.
		 * @param position the position to use.
		 */
		void execute(PAnimationEventContext context, PAnimationEventContext.PAnimationEventDispatcherBridge.Position position);
	}
}
