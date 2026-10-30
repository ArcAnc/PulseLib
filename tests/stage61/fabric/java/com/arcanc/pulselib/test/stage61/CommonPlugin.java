/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61;

import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/** Real BOTH-side external plugin with an observable registry contribution. */
@PulsePlugin(PulsePlugin.Side.BOTH)
public final class CommonPlugin implements IPulseCommonPlugin
{
	public CommonPlugin()
	{
		FabricBootstrapProbe.common++;
	}

	@Override
	public Identifier getPluginId()
	{
		return PLibDatabase.rl("stage61/common");
	}

	@Override
	public void registerAnimationChannels(PTypeRegistrar registrar)
	{
		FabricBootstrapProbe.channels++;
		registrar.registerAnimationChannel(new PAnimationChannel.Vector3fChannelType(
				PLibDatabase.rl("stage61/channel"), new Vector3f(), false));
	}

	@Override
	public void registerAnimationEvents(PTypeRegistrar registrar)
	{
		FabricBootstrapProbe.events++;
	}

	@Override
	public void registerDeformers(PTypeRegistrar registrar)
	{
		FabricBootstrapProbe.deformers++;
	}
}
