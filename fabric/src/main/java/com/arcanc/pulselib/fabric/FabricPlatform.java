package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.platform.IPulsePlatform;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Map;

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

	@Override
	public void registerModelResources(Map<ResourceLocation, PModelResource> resources)
	{
		PulsePluginManager.get().registerModels(resources);
	}
}
