/**
 * @author ArcAnc
 * Created at: 11.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;


import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

/**
 * Animatable item contract that exposes the PulseLib item renderer.
 *
 * @param <T> the animatable owner type
 */
public interface PItemAnimatable<T extends PAnimatable<T>> extends PAnimatable<T>
{
	/**
	 * Returns the custom item renderer used for this animatable item.
	 *
	 * @return the custom item renderer used for this animatable item
	 */
	default BlockEntityWithoutLevelRenderer getPulseItemRenderer()
	{
		return null;
	}
}
