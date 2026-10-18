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
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Tracks animation managers whose lifetime follows individual entity or block entity owners.
 * <p>
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/animatable/instance/InstancedAnimatableInstanceCache.java">InstancedAnimatableInstanceCache</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 *
 * @param <T> the animatable owner type
 */
public class InstanceAnimationManager<T extends PAnimatable<T>> extends PAnimationManager<T>
{
	private static final Map<InstanceAnimationManager<?>, Long> MANAGERS = new IdentityHashMap<>();

	/**
	 * Creates directly owned animation state for the supplied animatable.
	 *
	 * @param animatable the owner whose animation state is being processed
	 */
	public InstanceAnimationManager(T animatable)
	{
		this(animatable, AnimManagerKey.ofObject(animatable));
	}

	/**
	 * Creates directly owned animation state for the supplied animatable.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param key the key
	 */
	public InstanceAnimationManager(T animatable, AnimManagerKey key)
	{
		super(animatable, key);
	}

	/**
	 * Ticks every tracked instance animation manager.
	 */
	public static void tickAll()
	{
		MANAGERS.keySet().forEach(PAnimationManager :: tick);
	}

	/**
	 * Tracks an instance manager and records its most recent use time.
	 *
	 * @param manager the manager
	 */
	public static void addManager(PAnimationManager<?> manager)
	{
		addManager(manager, Util.getEpochMillis());
	}

	/**
	 * Package-private time seam shared by real rendering access and deterministic tests.
	 *
	 * @param manager the manager
	 * @param now the current manager-maintenance timestamp
	 */
	static void addManager(PAnimationManager<?> manager, long now)
	{
		if (manager instanceof InstanceAnimationManager<?> instance)
			MANAGERS.put(instance, now);
	}

	/**
	 * Removes instance managers whose idle time exceeds the retention threshold.
	 */
	public static void removeUnused()
	{
		ClientLevel level = PLibRenderHelper.mc().level;
		if (level == null)
			return;
		if (level.getGameTime() % 200 != 0)
			return;
		if (MANAGERS.isEmpty())
			return;

		removeUnused(Util.getEpochMillis());
	}

	/**
	 * Expires entries relative to their most recent access, retaining the existing 5s TTL.
	 *
	 * @param now the current manager-maintenance timestamp
	 */
	static void removeUnused(long now)
	{
		MANAGERS.entrySet().removeIf(entry -> entry.getValue() + THRESHOLD_TIME < now);
	}

	/**
	 * Discards all tracked instance animation managers.
	 */
	public static void cleanUp()
	{
		MANAGERS.clear();
	}

	/**
	 * Removes the animation manager associated with the supplied owner.
	 *
	 * @param animatable the owner whose animation state is being processed
	 */
	public static void removeManager(Object animatable)
	{
		MANAGERS.keySet().removeIf(manager -> manager.getAnimatable() == animatable);
	}
}
