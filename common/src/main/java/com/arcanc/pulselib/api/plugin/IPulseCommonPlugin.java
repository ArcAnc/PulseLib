package com.arcanc.pulselib.api.plugin;

import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import net.minecraft.resources.ResourceLocation;

public interface IPulseCommonPlugin
{
	ResourceLocation getPluginId();

	default void registerAnimationChannels(PTypeRegistrar registrar) { }

	default void registerAnimationEvents(PTypeRegistrar registrar) { }

	default void registerDeformers(PTypeRegistrar registrar) { }
}
