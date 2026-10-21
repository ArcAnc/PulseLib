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
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/** A real external common plugin; duplicate callback execution would duplicate its registry entry. */
@PulsePlugin(PulsePlugin.Side.BOTH)
public final class Stage2CommonPlugin implements IPulseCommonPlugin
{
	public Stage2CommonPlugin()
	{
		Stage2PluginProbe.commonConstructions++;
	}

	@Override
	public Identifier getPluginId()
	{
		return Stage2PluginProbe.COMMON_ID;
	}

	@Override
	public void registerAnimationChannels(PTypeRegistrar registrar)
	{
		Stage2PluginProbe.channels++;
		registrar.registerAnimationChannel(new PAnimationChannel.Vector3fChannelType(
				Stage2PluginProbe.CHANNEL_ID, new Vector3f(), false));
	}

	@Override
	public void registerAnimationEvents(PTypeRegistrar registrar)
	{
		Stage2PluginProbe.events++;
	}

	@Override
	public void registerDeformers(PTypeRegistrar registrar)
	{
		Stage2PluginProbe.deformers++;
	}
}
