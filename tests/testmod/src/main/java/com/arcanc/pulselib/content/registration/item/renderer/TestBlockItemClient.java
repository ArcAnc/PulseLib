package com.arcanc.pulselib.content.registration.item.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

/** Keeps client renderer construction out of the shared item's bytecode. */
public final class TestBlockItemClient
{
	private static TestBlockItemRenderer renderer;

	private TestBlockItemClient() { }

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
