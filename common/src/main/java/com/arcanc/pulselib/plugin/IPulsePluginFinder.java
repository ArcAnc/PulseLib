/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.plugin;

import java.util.List;

/**
 * Discovers annotated plugin entry points without instantiating their classes.
 */
public interface IPulsePluginFinder
{
	/**
	 * Returns discovered plugin class names, owning mods and physical-side restrictions.
	 *
	 * @return discovered plugin class names, owning mods and physical-side restrictions
	 */
	List<PulsePluginCandidate> findPlugins();
}
