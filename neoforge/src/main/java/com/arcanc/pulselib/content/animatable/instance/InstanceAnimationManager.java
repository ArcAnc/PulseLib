/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable.instance;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Util;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Objects;
import java.util.Set;

/**
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/animatable/instance/InstancedAnimatableInstanceCache.java">InstancedAnimatableInstanceCache</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 */
@EventBusSubscriber (modid = PLibDatabase.MOD_ID)
public class InstanceAnimationManager<T extends PAnimatable<T>> extends PAnimationManager<T>
{
	private static final Set<AnimationManagerContainer<?>> MANAGERS = new ObjectOpenHashSet<>();
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param animatable the animatable to use.
	 */
	public InstanceAnimationManager(T animatable)
	{
		this(animatable, AnimManagerKey.ofObject(animatable));
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param animatable the animatable to use.
	 * @param key the key to use.
	 */
	public InstanceAnimationManager(T animatable, AnimManagerKey key)
	{
		super(animatable, key);
		createControllers();
	}
	
	/**
	 * Updates the all.
	 */
	public static void tickAll()
	{
		MANAGERS.forEach(container ->
				container.manager().tick());
	}
	
	/**
	 * Adds the manager.
	 * @param manager the manager to use.
	 */
	public static void addManager(PAnimationManager<?> manager)
	{
		if (manager instanceof InstanceAnimationManager<?> instance)
			MANAGERS.add(new AnimationManagerContainer<>(Util.getEpochMillis(), instance));
	}
	
	/**
	 * Removes the unused.
	 * @param event the event to use.
	 */
	@SubscribeEvent
	public static void removeUnused(final ClientTickEvent.Post event)
	{
		ClientLevel level = PLibRenderHelper.mc().level;
		if (level == null)
			return;
		if (level.getGameTime() % 200 != 0)
			return;
		if (MANAGERS.isEmpty())
			return;
		
		long now = Util.getEpochMillis();
		
		MANAGERS.removeIf(container -> container.lastUsedTick() + THRESHOLD_TIME < now);
	}
	
	/**
	 * Performs the clean up operation.
	 */
	public static void cleanUp()
	{
		MANAGERS.clear();
	}
	
/**
 * Provides support for animation manager container.
 */
	static class AnimationManagerContainer<T extends PAnimatable<T>>
	{
		long lastUsedTick;
		InstanceAnimationManager<T> manager;
		
		/**
		 * Creates an instance of the enclosing type.
		 * @param lastUsedTick the last used tick to use.
		 * @param manager the manager to use.
		 */
		AnimationManagerContainer(long lastUsedTick, InstanceAnimationManager<T> manager)
		{
			this.lastUsedTick = lastUsedTick;
			this.manager = manager;
		}
		
		/**
		 * Performs the manager operation.
		 * @return the value produced by this operation.
		 */
		InstanceAnimationManager<T> manager()
		{
			return this.manager;
		}
		
		/**
		 * Performs the last used tick operation.
		 * @return the value produced by this operation.
		 */
		long lastUsedTick()
		{
			return this.lastUsedTick;
		}
		
		/**
		 * Performs the equals operation.
		 * @param o the o to use.
		 * @return the value produced by this operation.
		 */
		@Override
		public boolean equals(Object o)
		{
			if (! (o instanceof AnimationManagerContainer<?> that))
				return false;
			return Objects.equals(this.manager, that.manager);
		}
		
		/**
		 * Performs the hash code operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public int hashCode()
		{
			return Objects.hashCode(this.manager);
		}
	}
}
