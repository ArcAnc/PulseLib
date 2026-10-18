/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Defines the payload codec, execution side and handler of an animation event.
 *
 * @param <T> the decoded animation event payload type
 */
public interface PAnimationEventType<T>
{
	/**
	 * Returns the namespaced registry identifier of this event type.
	 *
	 * @return the namespaced registry identifier of this event type
	 */
	ResourceLocation id();

	/**
	 * Returns the codec used to decode this event payload.
	 *
	 * @return the codec used to decode this event payload
	 */
	MapCodec<T> codec();

	/**
	 * Returns the logical sides on which this event can execute.
	 *
	 * @return the logical sides on which this event can execute
	 */
	PEventSide side();

	/**
	 * Handles the decoded event payload in the supplied animation context.
	 *
	 * @param context the current event owner, controller, level and locator context
	 * @param data the decoded event payload
	 */
	void execute(PAnimationEventContext context, T data);
}
