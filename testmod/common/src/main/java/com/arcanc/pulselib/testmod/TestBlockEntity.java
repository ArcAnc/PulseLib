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
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TestBlockEntity extends BlockEntity implements PAnimatable<TestBlockEntity>
{
	private static final PRawAnimation LOOP = PRawAnimation.begin().thenLoop("animation").build();

	private final PAnimationManager<TestBlockEntity> animationManager = PLibHelper.createManager(this);

	public TestBlockEntity(BlockPos pos, BlockState state)
	{
		super(TestContent.blockEntity(), pos, state);
	}

	@Override
	public PAnimationManager<TestBlockEntity> getAnimationManager(AnimManagerKey key)
	{
		return this.animationManager;
	}

	@Override
	public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<TestBlockEntity> registrar)
	{
		registrar.add("loop", () -> state ->
		{
			state.controller().play(LOOP);
			return ControllerState.PLAY;
		});
	}
}
