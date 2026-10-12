package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.*;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.renderer.PItemRenderer;
import com.arcanc.pulselib.util.PRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;

public class TestItem extends Item implements PItemAnimatable<TestItem>
{
	public static int renders;
	public TestItem() { super(new Properties()); }
	@Override public PAnimationManager<TestItem> getAnimationManager(AnimManagerKey key) { return SingletonAnimationManager.getManager(key, this); }
	@Override public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<TestItem> registrar)
	{
		registrar.add("idle", () -> state -> ControllerState.PLAY);
	}
	@Override public BlockEntityWithoutLevelRenderer getPulseItemRenderer()
	{
		return TestItemClient.renderer(this);
	}
}
