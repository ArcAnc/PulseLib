package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.*;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.util.PRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;

public final class TestItemClient
{
	private static final java.util.Map<TestItem, BlockEntityWithoutLevelRenderer> RENDERERS = new java.util.IdentityHashMap<>();
	private TestItemClient() { }
	public static BlockEntityWithoutLevelRenderer renderer(TestItem item)
	{
		BlockEntityWithoutLevelRenderer renderer = RENDERERS.get(item);
		if (renderer == null)
		{
			Minecraft mc = Minecraft.getInstance();
			renderer = new PItemRenderer<TestItem>(ClientTestPlugin.GLTF,
					PRenderTypes.RenderTypeProvider::trianglesSolidEmissive, mc.getBlockEntityRenderDispatcher(), mc.getEntityModels())
			{
				@Override public void preSubmit(com.mojang.blaze3d.vertex.PoseStack poses, TestItem animatable,
						java.util.function.Function<net.minecraft.resources.ResourceLocation, net.minecraft.client.renderer.RenderType> type,
						net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay, float partialTick, Object... data)
				{
					TestItem.renders++;
				}
			};
		}
		RENDERERS.put(item, renderer);
		return renderer;
	}
}
