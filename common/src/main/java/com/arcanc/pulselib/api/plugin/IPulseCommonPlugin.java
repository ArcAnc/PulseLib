/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.api.plugin;

import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import net.minecraft.resources.ResourceLocation;

public interface IPulseCommonPlugin
{
	ResourceLocation getPluginId();

	default void registerAnimationChannels(PTypeRegistrar registrar)
	{
	}

	default void registerAnimationEvents(PTypeRegistrar registrar)
	{
	}

	default void registerDeformers(PTypeRegistrar registrar)
	{
	}
}
