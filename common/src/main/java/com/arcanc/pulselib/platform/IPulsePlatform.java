package com.arcanc.pulselib.platform;

import net.minecraft.world.entity.Entity;

public interface IPulsePlatform
{
	boolean isClient();
	boolean shouldRiderSit(Entity mount);
}
