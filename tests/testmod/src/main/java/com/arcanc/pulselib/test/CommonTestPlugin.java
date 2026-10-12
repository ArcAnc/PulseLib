package com.arcanc.pulselib.test;

import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

@PulsePlugin(PulsePlugin.Side.BOTH)
public class CommonTestPlugin implements IPulseCommonPlugin
{
	public static int constructions;
	public static int registrations;

	public CommonTestPlugin()
	{
		if (++constructions != 1)
			throw new AssertionError("Common plugin constructed more than once");
		PLibDatabase.LOGGER.info("PULSE TEST common plugin constructed");
	}

	@Override
	public ResourceLocation getPluginId()
	{
		return ResourceLocation.fromNamespaceAndPath("pulselibtest", "common");
	}

	@Override
	public void registerAnimationChannels(PTypeRegistrar registrar)
	{
		if (++registrations != 1)
			throw new AssertionError("Common channel phase repeated");
		registrar.registerAnimationChannel(new PAnimationChannel.Vector3fChannelType(
				ResourceLocation.fromNamespaceAndPath("pulselibtest", "offset"), new Vector3f(), false));
		PLibDatabase.LOGGER.info("PULSE TEST common channel registered");
	}
}
