/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.helpers;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.renderer.base.PEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class PLibHelper
{
	/**
	 * Creates the manager.
	 * @param animatable the animatable to use.
	 * @return the value produced by this operation.
	 */
	public static <T extends PAnimatable<T>> PAnimationManager<T> createManager(T animatable)
	{
		if (animatable instanceof BlockEntity || animatable instanceof Entity)
			return createManager(animatable, false);
		return createManager(animatable, true);
	}
	
	/**
	 * Creates the manager.
	 * @param animatable the animatable to use.
	 * @param singleton the singleton to use.
	 * @return the value produced by this operation.
	 */
	public static <T extends PAnimatable<T>> PAnimationManager<T> createManager(T animatable, boolean singleton)
	{
		AnimManagerKey key = AnimManagerKey.ofObject(animatable);
		PAnimationManager<T> manager = animatable.getAnimationManager(key);
		
		if (manager != null)
			return manager;
		
		return singleton ? SingletonAnimationManager.getManager(key, animatable) : new InstanceAnimationManager<>(animatable);
	}
	
	/**
	 * Performs the living render state operation.
	 * @param factory the factory to use.
	 * @return the value produced by this operation.
	 */
	public static <T extends LivingEntity & PAnimatable<T>, RS extends PEntityRenderState<T>> RS livingRenderState(Supplier<RS> factory)
	{
		return factory.get();
	}

	/**
	 * Performs the living render state operation.
	 * @return the value produced by this operation.
	 */
	public static <T extends LivingEntity & PAnimatable<T>> PEntityRenderState.LivingImpl<T> livingRenderState()
	{
		return livingRenderState(PEntityRenderState.LivingImpl::new);
	}
	
	/**
	 * Performs the entity render state operation.
	 * @param factory the factory to use.
	 * @return the value produced by this operation.
	 */
	public static <T extends Entity & PAnimatable<T>, RS extends PEntityRenderState<T>> RS entityRenderState(Supplier<RS> factory)
	{
		return factory.get();
	}

	/**
	 * Performs the entity render state operation.
	 * @return the value produced by this operation.
	 */
	public static <T extends Entity & PAnimatable<T>> PEntityRenderState.Impl<T> entityRenderState()
	{
		return entityRenderState(PEntityRenderState.Impl::new);
	}
}
