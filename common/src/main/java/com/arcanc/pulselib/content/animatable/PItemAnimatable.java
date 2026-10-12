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

public interface PItemAnimatable<T extends PAnimatable<T>> extends PAnimatable<T>
{
	default BlockEntityWithoutLevelRenderer getPulseItemRenderer()
	{
		return null;
	}
}
