/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public final class TestItem extends BlockItem implements PAnimatable<TestItem>
{
	private static final PRawAnimation IDLE = PRawAnimation.begin().thenLoop("idle").build();

	public TestItem(Block block, Properties properties)
	{
		super(block, properties);
	}

	@Override
	public PAnimationManager<TestItem> getAnimationManager(AnimManagerKey key)
	{
		return SingletonAnimationManager.getManager(key, this);
	}

	@Override
	public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<TestItem> registrar)
	{
		registrar.add("idle", () -> state ->
		{
			state.controller().play(IDLE);
			return ControllerState.PLAY;
		});
	}
}
