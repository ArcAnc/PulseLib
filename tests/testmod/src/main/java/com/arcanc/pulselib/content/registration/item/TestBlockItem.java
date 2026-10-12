/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.item;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.PItemAnimatable;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemClient;
import org.jetbrains.annotations.NotNull;

public class TestBlockItem extends BlockItem implements PItemAnimatable<TestBlockItem>
{
	private final PRawAnimation idle = PRawAnimation.begin().
			thenLoop("idle").
			build();
	
	public TestBlockItem(Block block, Properties properties)
	{
		super(block, properties);
	}
	
	@Override
	public @NotNull PAnimationManager<TestBlockItem> getAnimationManager(AnimManagerKey key)
	{
		return SingletonAnimationManager.getManager(key, this);
	}
	
	@Override
	public void registerAnimationControllers(PAnimationManager.@NotNull PAnimationRegistrar<TestBlockItem> registrar)
	{
		registrar.add(() -> state ->
		{
			state.controller().play(this.idle);
			return ControllerState.PLAY;
		});
	}
	
	@Override
	public BlockEntityWithoutLevelRenderer getPulseItemRenderer()
	{
		return TestBlockItemClient.renderer();
	}
}
