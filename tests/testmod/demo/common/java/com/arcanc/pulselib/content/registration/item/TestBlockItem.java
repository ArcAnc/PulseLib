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
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

public class TestBlockItem extends BlockItem implements PAnimatable<TestBlockItem>
{
	private final PAnimationManager<TestBlockItem> animationManager = PLibHelper.createManager(this);
	private final PRawAnimation idle = PRawAnimation.begin().
			thenLoop("idle").
			build();

	public TestBlockItem(Block block, Properties properties)
	{
		super(block, properties);
	}

	@Override
	public @NonNull PAnimationManager<TestBlockItem> getAnimationManager(AnimManagerKey key)
	{
		return this.animationManager.getManager(key);
	}

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
