/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.content.event.ClientEvents;
import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;

/** Physical-client provider for the canonical client lifecycle, without changing its callbacks. */
@ApiStatus.Internal
public final class NeoForgeClientBootstrap implements INeoForgeClientBootstrap
{
	@Override
	public void register(IEventBus bus)
	{
		ClientEvents.registerClientEvents(bus);
	}
}
