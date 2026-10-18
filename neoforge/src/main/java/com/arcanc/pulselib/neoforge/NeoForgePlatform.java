/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.platform.IPulsePlatform;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.loading.FMLLoader;

/**
 * NeoForge implementation of common physical-side and entity riding queries.
 */
public final class NeoForgePlatform implements IPulsePlatform
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isClient()
	{
		return FMLLoader.getDist().isClient();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean shouldRiderSit(Entity mount)
	{
		return mount.shouldRiderSit();
	}
}
