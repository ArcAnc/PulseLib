/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;

/** Declares the white atlas control through the same public model registration path as consumers. */
@PulsePlugin
public final class DeformationPlugin implements IPulseClientPlugin
{
	public static final PModelData MODEL = PModelData.direct(PLibDatabase.rl("stage62d2b/white"), PGeckoModelLoader.INSTANCE.id());
	public static int collections;

	@Override
	public Identifier getPluginId()
	{
		return PLibDatabase.rl("stage62d2b/client");
	}

	@Override
	public void registerModels(PModelRegistrar registrar)
	{
		collections++;
		registrar.model(MODEL).texture("0", PLibDatabase.rl("stage62d2b_white"));
	}
}
