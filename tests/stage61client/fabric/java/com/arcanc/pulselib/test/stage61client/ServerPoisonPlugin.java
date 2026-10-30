/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client;

import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;

/** Must not be class-loaded by the physical-client plugin manager. */
@PulsePlugin(PulsePlugin.Side.SERVER)
public final class ServerPoisonPlugin implements IPulseCommonPlugin
{
	public ServerPoisonPlugin()
	{
		throw new AssertionError("SERVER plugin constructed on client");
	}

	@Override
	public Identifier getPluginId()
	{
		return PLibDatabase.rl("stage61client/server_poison");
	}
}
