/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;

@PulsePlugin
public final class ClientTestPlugin implements IPulseClientPlugin
{
	static
	{
		BootstrapChecks.check(PulseServices.PLATFORM.isClient(), "Client plugin class must never initialize on the server");
	}

	public static final PModelData GLTF = PModelData.item(PLibDatabase.rl("test_block"));
	public static final PModelData GECKO = PModelData.direct(PLibDatabase.rl("player/demo/player_flip"), PGeckoModelLoader.INSTANCE.id());
	public static int constructions;
	public static int models;

	public ClientTestPlugin()
	{
		BootstrapChecks.check(++constructions == 1, "Client plugin constructed exactly once");
	}

	@Override
	public Identifier getPluginId()
	{
		return BootstrapChecks.id("client");
	}

	@Override
	public void registerModels(PModelRegistrar registrar)
	{
		models++;
		registrar.model(GLTF).
				texture("pyramid", PLibDatabase.rl("item/test_block/pyramid")).
				texture("circle", PLibDatabase.rl("item/test_block/circle"));
		registrar.model(GECKO).
				texture("0", PLibDatabase.rl("entity/test_entity/0"));
	}
}
