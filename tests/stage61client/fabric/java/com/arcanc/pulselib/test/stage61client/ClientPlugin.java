/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;

/** External model declarations for the CPU and opt-in textured atlas probes. */
@PulsePlugin
public final class ClientPlugin implements IPulseClientPlugin
{
	@Override
	public Identifier getPluginId()
	{
		return PLibDatabase.rl("stage61client/client");
	}

	@Override
	public void registerAttachments(PAttachmentRegistrar registrar)
	{
		if (ClientProbe.common != 1)
			throw new AssertionError("Common registration must precede client registration");
		ClientProbe.client++;
	}

	@Override
	public void registerPlayerAnimations(PPlayerAnimationRegistrar registrar)
	{
		if (Boolean.getBoolean("pulselib.fabricWorldProbe"))
			WorldProbe.animations(registrar);
	}

	@Override
	public void registerModels(PModelRegistrar registrar)
	{
		ClientProbe.models++;
		if (Boolean.getBoolean("pulselib.fabricStagesProbe"))
			WorldStageProbe.models(registrar);
		else if (Boolean.getBoolean("pulselib.fabricAtlasProbe"))
			AtlasProbe.models(registrar);
		else
			registrar.model(ClientProbe.DATA);
	}
}
