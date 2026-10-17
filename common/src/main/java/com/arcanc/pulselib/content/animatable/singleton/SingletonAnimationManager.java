/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable.singleton;


import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Map;

/**
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/animatable/instance/SingletonAnimatableInstanceCache.java">SingletonAnimatableInstanceCache</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 */

public class SingletonAnimationManager<T extends PAnimatable<T>> extends PAnimationManager<T>
{
	private static final Map<OwnerKey, AnimationManagerContainer<?>> MANAGERS = new Object2ObjectOpenHashMap<>();
	
	public SingletonAnimationManager(T animatable)
	{
		this(animatable, AnimManagerKey.ofObject(animatable));
	}

	public SingletonAnimationManager(T animatable, AnimManagerKey key)
	{
		super(animatable, key);
	}

	/** Resolves a context in the shared cache, including for non-Item singleton owners. */
	@Override
	public PAnimationManager<T> getManager(AnimManagerKey key)
	{
		return getManager(key, this.animatable);
	}
	
	@SuppressWarnings("unchecked")
	public static synchronized <T extends PAnimatable<T>> PAnimationManager<T> getManager(AnimManagerKey id, T animatable)
	{
		AnimationManagerContainer<?> cont = MANAGERS.compute(new OwnerKey(animatable, id), (key, container) ->
		{
			if (container == null)
			{
				SingletonAnimationManager<T> man = new SingletonAnimationManager<>(animatable, id);
				return new AnimationManagerContainer<>(Util.getEpochMillis(), man);
			}
			container.lastUsedTick = Util.getEpochMillis();
			return container;
		});
		
		// OwnerKey compares owner references, so this localized cast preserves T.
		return ((SingletonAnimationManager<T>)cont.manager()).initializeControllers();
	}
	
	public static void tickAll()
	{
		MANAGERS.values().forEach(container ->
				container.manager().tick());
	}
	
	public static void removeUnused()
	{
		ClientLevel level = PLibRenderHelper.mc().level;
		if (level == null)
			return;
		if (level.getGameTime() % 200 != 0)
			return;
		if (MANAGERS.isEmpty())
			return;
		
		long now = Util.getEpochMillis();
		
		MANAGERS.entrySet().removeIf(entry -> entry.getValue().lastUsedTick() + THRESHOLD_TIME < now);
	}
	
	public static void cleanUp()
	{
		MANAGERS.clear();
	}
	
	private record OwnerKey(PAnimatable<?> owner, AnimManagerKey key)
	{
		@Override
		public boolean equals(Object object)
		{
			return object instanceof OwnerKey other && this.owner == other.owner && this.key.equals(other.key);
		}

		@Override
		public int hashCode()
		{
			return 31 * System.identityHashCode(this.owner) + this.key.hashCode();
		}
	}

	static class AnimationManagerContainer<T extends PAnimatable<T>>
	{
		long lastUsedTick;
		SingletonAnimationManager<T> manager;
		
		AnimationManagerContainer(long lastUsedTick, SingletonAnimationManager<T> manager)
		{
			this.lastUsedTick = lastUsedTick;
			this.manager = manager;
		}
		
		SingletonAnimationManager<T> manager()
		{
			return this.manager;
		}
		
		long lastUsedTick()
		{
			return this.lastUsedTick;
		}
	}
}
