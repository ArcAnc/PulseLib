package com.arcanc.pulselib.platform;

import com.arcanc.pulselib.content.model.resource.PModelResource;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public interface IPulsePlatform
{
	boolean isClient();

	boolean shouldRiderSit(net.minecraft.world.entity.Entity mount);

	void registerModelResources(Map<ResourceLocation, PModelResource> resources);
}
