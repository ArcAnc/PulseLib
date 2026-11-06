/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.platform.PulseServices;
import net.minecraft.resources.Identifier;

@PulsePlugin(PulsePlugin.Side.SERVER)

public final class ServerTestPlugin implements IPulseCommonPlugin
{
	public ServerTestPlugin()
	{
		DemoChecks.check(!PulseServices.PLATFORM.isClient(), "Server plugin stays on physical server");
		DemoChecks.serverPlugins++;
	}

	@Override
	public Identifier getPluginId()
	{
		return DemoChecks.id("server");
	}
}
