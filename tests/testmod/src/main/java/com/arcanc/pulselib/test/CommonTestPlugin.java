package com.arcanc.pulselib.test;

import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.ResourceLocation;

/** Exercises each registrar using existing built-in implementations, without invented types. */
@PulsePlugin(PulsePlugin.Side.BOTH)
public class CommonTestPlugin implements IPulseCommonPlugin
{
	public static int constructions;
	public static int registrations;
	public static int events;
	public static int deformers;

	public CommonTestPlugin()
	{
		if (++constructions != 1)
			throw new AssertionError("Common plugin constructed more than once");
		PLibDatabase.LOGGER.info("PULSE TEST common plugin constructed");
	}

	@Override public ResourceLocation getPluginId() { return DemoContent.id("common"); }

	@Override public void registerAnimationChannels(PTypeRegistrar registrar)
	{
		if (++registrations != 1) throw new AssertionError("Channel phase repeated");
		registrar.registerAnimationChannel(PLibRegistration.AnimationChannelReg.POSITION);
	}

	@Override public void registerAnimationEvents(PTypeRegistrar registrar)
	{
		if (++events != 1) throw new AssertionError("Event phase repeated");
		registrar.registerAnimationEvent(PLibRegistration.AnimationEventReg.SOUND);
	}

	@Override public void registerDeformers(PTypeRegistrar registrar)
	{
		if (++deformers != 1) throw new AssertionError("Deformer phase repeated");
		registrar.registerMeshDeformer(PLibRegistration.MeshDeformerReg.BEND);
	}
}
