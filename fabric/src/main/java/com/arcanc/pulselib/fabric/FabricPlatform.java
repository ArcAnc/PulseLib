package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.platform.IPulsePlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;


public final class FabricPlatform implements IPulsePlatform
{
	@Override
	public boolean isClient()
	{
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}

	@Override
	public boolean shouldRiderSit(Entity mount)
	{
		return true;
	}

}
