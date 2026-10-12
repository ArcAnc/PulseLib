/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.item.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps client renderer construction out of the shared item's bytecode.
 */
public final class TestBlockItemClient
{
	private static @Nullable TestBlockItemRenderer renderer;

	private TestBlockItemClient()
	{
	}

	public static BlockEntityWithoutLevelRenderer renderer()
	{
		if (renderer == null)
		{
			Minecraft client = Minecraft.getInstance();
			renderer = new TestBlockItemRenderer(client.getBlockEntityRenderDispatcher(), client.getEntityModels());
		}
		return renderer;
	}
}
