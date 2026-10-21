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
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/**
 * Provides support for test block item.
 */
public class TestBlockItem extends BlockItem implements PAnimatable<TestBlockItem>
{
	private final PRawAnimation idle = PRawAnimation.begin().
			thenLoop("idle").
			build();
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param block the block to use.
	 * @param properties the properties to use.
	 */
	public TestBlockItem(Block block, Properties properties)
	{
		super(block, properties);
	}
	
	/**
	 * Returns the animation manager.
	 * @param key the key to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public PAnimationManager<TestBlockItem> getAnimationManager(AnimManagerKey key)
	{
		return SingletonAnimationManager.getManager(key, this);
	}
	
	/**
	 * Registers the animation controllers.
	 * @param registrar the registrar to use.
	 */
	@Override
	public void registerAnimationControllers(PAnimationManager.@NonNull PAnimationRegistrar<TestBlockItem> registrar)
	{
		registrar.add(() -> state ->
		{
			state.controller().play(this.idle);
			return ControllerState.PLAY;
		});
	}
}
