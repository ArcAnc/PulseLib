package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.content.event.PulseLibEvents;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.platform.IPulsePlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLLoader;

import java.util.Map;

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

	@Override
	public void registerModelResources(Map<ResourceLocation, PModelResource> resources)
	{
		com.arcanc.pulselib.content.event.ClientEvents.ensurePulseClientContentRegistered();
		com.arcanc.pulselib.plugin.PulsePluginManager.get().registerModels(resources);
		PulseLibEvents.RegisterResourceEvent event = new PulseLibEvents.RegisterResourceEvent();
		ModLoader.postEvent(event);
		event.apply(resources);
	}
}
