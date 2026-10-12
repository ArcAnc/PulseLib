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
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.util.PLibDatabase;
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
	private static final Map<AnimManagerKey, AnimationManagerContainer<?>> MANAGERS = new Object2ObjectOpenHashMap<>();
	
	public SingletonAnimationManager(T animatable)
	{
		this(animatable, AnimManagerKey.ofObject(animatable));
	}

	public SingletonAnimationManager(T animatable, AnimManagerKey key)
	{
		super(animatable, key);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends PAnimatable<T>> PAnimationManager<T> getManager(AnimManagerKey id, T animatable)
	{
		AnimationManagerContainer<?> cont = MANAGERS.compute(id, (key, container) ->
		{
			if (container == null)
			{
				SingletonAnimationManager<T> man = new SingletonAnimationManager<>(animatable, key);
				man.createControllers();
				return new AnimationManagerContainer<>(Util.getEpochMillis(), man);
			}
			container.lastUsedTick = Util.getEpochMillis();
			return container;
		});
		
		return (SingletonAnimationManager<T>)cont.manager();
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
