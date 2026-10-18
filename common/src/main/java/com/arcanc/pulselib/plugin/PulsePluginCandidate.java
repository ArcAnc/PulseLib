/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.plugin;

import com.arcanc.pulselib.api.plugin.PulsePlugin;

import java.util.Objects;

/**
 * Discovery metadata, obtained without loading the plugin class.
 *
 * @param className the binary name of the discovered plugin class
 * @param modId the owning mod identifier used in discovery diagnostics
 * @param side the physical-side restriction of the plugin candidate
 */
public record PulsePluginCandidate(String className, String modId, PulsePlugin.Side side)
{
	/**
	 * Captures a discovered plugin class without loading or initializing it.
	 *
	 * @param className the binary name of the discovered plugin class
	 * @param modId the owning mod identifier used in discovery diagnostics
	 * @param side the physical-side restriction of the plugin candidate
	 */
	public PulsePluginCandidate
	{
		Objects.requireNonNull(className);
		Objects.requireNonNull(modId);
		Objects.requireNonNull(side);
	}
}
