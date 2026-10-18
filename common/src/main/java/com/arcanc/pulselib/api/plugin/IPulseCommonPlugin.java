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

/**
 * Registers animation channel, event and mesh deformer types available on both physical sides. The plugin
 * id identifies the registration owner; callbacks run in the lifecycle order managed by PulseLib.
 */
public interface IPulseCommonPlugin
{
	/**
	 * Returns the stable, namespaced identifier of this plugin.
	 *
	 * @return the stable, namespaced identifier of this plugin
	 */
	ResourceLocation getPluginId();

	/**
	 * Registers custom animation channel types during the channel registration phase.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	default void registerAnimationChannels(PTypeRegistrar registrar)
	{
	}

	/**
	 * Registers custom animation event types during the event registration phase.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	default void registerAnimationEvents(PTypeRegistrar registrar)
	{
	}

	/**
	 * Registers custom mesh deformer types during the deformer registration phase.
	 *
	 * @param registrar the active registration scope for this callback
	 */
	default void registerDeformers(PTypeRegistrar registrar)
	{
	}
}
