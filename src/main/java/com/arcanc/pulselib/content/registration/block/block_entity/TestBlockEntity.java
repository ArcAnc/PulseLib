/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.block.block_entity;


import com.arcanc.pulselib.content.animatable.*;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Provides support for test block entity.
 */
public class TestBlockEntity extends BlockEntity implements PAnimatable<TestBlockEntity>
{
	private final PAnimationManager<TestBlockEntity> animationManager = PLibHelper.createManager(this);
	private final PRawAnimation ANIMATION = PRawAnimation.begin().
			thenRandomLoop(random -> random.
					add("animation", 5).
					add("animation2", 5).
					preventImmediateRepeat()).
			build();
	private boolean playAnimation = true;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param pos the pos to use.
	 * @param blockState the block state to use.
	 */
	public TestBlockEntity(BlockPos pos, BlockState blockState)
	{
		super(PLibRegistration.BETypeReg.TEST_BLOCK_ENTITY.get(), pos, blockState);
	}
	
	/**
	 * Performs the change play animation operation.
	 */
	public void changePlayAnimation()
	{
		this.playAnimation = !playAnimation;
		this.setChanged();
	}
	
	/**
	 * Determines whether play animation.
	 * @return the value produced by this operation.
	 */
	public boolean isPlayAnimation()
	{
		return this.playAnimation;
	}
	
	/**
	 * Returns the animation manager.
	 * @param key the key to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public PAnimationManager<TestBlockEntity> getAnimationManager(AnimManagerKey key)
	{
		return this.animationManager;
	}
	
	/**
	 * Registers the animation controllers.
	 * @param registrar the registrar to use.
	 */
	@Override
	public void registerAnimationControllers(PAnimationManager.@NotNull PAnimationRegistrar<TestBlockEntity> registrar)
	{
		registrar.add(() -> animatableState ->
		{
			TestBlockEntity blockEntity = animatableState.animatable();
			PAnimationController<TestBlockEntity> controller = animatableState.controller();
			if (blockEntity.isPlayAnimation())
			{
				controller.play(ANIMATION);
				return ControllerState.PLAY;
			}
			else
			{
				controller.stop();
				return ControllerState.STOP;
			}
		});
	}
}
