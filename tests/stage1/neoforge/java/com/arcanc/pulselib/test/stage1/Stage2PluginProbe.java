/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.Identifier;

import java.util.Set;

/** Cross-side assertions use data only; never reference the opposite-side plugin class. */
public final class Stage2PluginProbe
{
	public static final Identifier COMMON_ID = PLibDatabase.rl("stage2_common");
	public static final Identifier SERVER_ID = PLibDatabase.rl("stage2_server");
	public static final Identifier CLIENT_ID = PLibDatabase.rl("stage1_client");
	public static final Identifier CHANNEL_ID = PLibDatabase.rl("stage2_probe_channel");
	public static volatile int commonConstructions;
	public static volatile int serverConstructions;
	public static volatile int channels;
	public static volatile int events;
	public static volatile int deformers;
	public static volatile int clientConstructions;
	public static volatile int attachments;
	public static volatile int playerAnimations;
	public static volatile int animatedAttachments;
	public static volatile int models;

	private Stage2PluginProbe()
	{
	}

	public static void verify(boolean client)
	{
		PulsePluginManager manager = PulsePluginManager.get();
		manager.registerCommon();
		if (!Set.copyOf(manager.pluginIds()).equals(Set.of(COMMON_ID, client ? CLIENT_ID : SERVER_ID)))
		{
			throw new AssertionError("Unexpected plugins on side client=" + client + ": " + manager.pluginIds());
		}
		if (commonConstructions != 1 || channels != 1 || events != 1 || deformers != 1 ||
				PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(CHANNEL_ID).isEmpty())
		{
			throw new AssertionError("Common registration did not run exactly once");
		}
		if (client)
		{
			manager.registerClient();
			if (serverConstructions != 0 || clientConstructions != 1 || attachments != 1 ||
					playerAnimations != 1 || animatedAttachments != 1 || models != 2)
			{
				throw new AssertionError("Client lifecycle counters: constructions=" + clientConstructions +
						", models=" + models + ", attachments=" + attachments + ", player=" + playerAnimations +
						", animated=" + animatedAttachments + ", server=" + serverConstructions);
			}
		}
		else
		{
			if (serverConstructions != 1 || clientConstructions != 0 || models != 0)
			{
				throw new AssertionError("Server loaded client plugin content");
			}
			try
			{
				manager.registerClient();
				throw new AssertionError("Dedicated server accepted a client registration phase");
			}
			catch (IllegalStateException expected)
			{
				// The public contract rejects this phase before entering client registrars.
			}
		}
		PLibDatabase.LOGGER.info("STAGE 2 {} PLUGINS PASS: ids={}, common=1, channels=1, events=1, deformers=1, client={}, server={}, models={}",
				client ? "CLIENT" : "SERVER", manager.pluginIds(), clientConstructions, serverConstructions, models);
	}
}
