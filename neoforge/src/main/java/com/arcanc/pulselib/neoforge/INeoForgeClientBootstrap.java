/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.neoforge;

import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;

/** Delays resolution of client lifecycle implementations until the physical client is initialized. */
@ApiStatus.Internal
public interface INeoForgeClientBootstrap
{
	/** Installs the existing NeoForge client callbacks on the mod event bus. */
	void register(IEventBus bus);
}
