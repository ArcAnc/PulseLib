/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;

/** Must be scanned as metadata and never initialized on the physical server. */
@PulsePlugin
public final class ClientPoisonPlugin implements IPulseClientPlugin
{
	public ClientPoisonPlugin()
	{
		throw new AssertionError("Client plugin instantiated on the dedicated server");
	}

	@Override
	public Identifier getPluginId()
	{
		return PLibDatabase.rl("stage61/client_poison");
	}
}
