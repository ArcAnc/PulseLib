/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.platform.PulseServices;
import net.minecraft.resources.Identifier;

/** Must never be class-loaded by the physical client plugin manager. */
@PulsePlugin(PulsePlugin.Side.SERVER)
public final class Stage2ServerPlugin implements IPulseCommonPlugin
{
	public Stage2ServerPlugin()
	{
		if (PulseServices.PLATFORM.isClient())
		{
			throw new AssertionError("Server-only plugin instantiated on the client");
		}
		Stage2PluginProbe.serverConstructions++;
	}

	@Override
	public Identifier getPluginId()
	{
		return Stage2PluginProbe.SERVER_ID;
	}
}
