package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.platform.IPulsePlatform;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.loading.FMLLoader;


public final class NeoForgePlatform implements IPulsePlatform
{
	@Override
	public boolean isClient()
	{
		return FMLLoader.getDist().isClient();
	}

	@Override
	public boolean shouldRiderSit(Entity mount)
	{
		return mount.shouldRiderSit();
	}

}
