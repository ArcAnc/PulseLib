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
import net.minecraft.resources.Identifier;

/**
 * Defines the contract for animation event type.
 */
public interface PAnimationEventType<T>
{
	/**
	 * Performs the id operation.
	 * @return the value produced by this operation.
	 */
	Identifier id();

	/**
	 * Performs the codec operation.
	 * @return the value produced by this operation.
	 */
	MapCodec<T> codec();

	/**
	 * Performs the side operation.
	 * @return the value produced by this operation.
	 */
	PEventSide side();

	/**
	 * Performs the execute operation.
	 * @param context the context to use.
	 * @param data the data to use.
	 */
	void execute(PAnimationEventContext context, T data);
}
