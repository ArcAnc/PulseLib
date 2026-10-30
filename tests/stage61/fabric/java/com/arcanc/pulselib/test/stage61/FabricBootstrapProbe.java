/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61;

import com.arcanc.pulselib.fabric.FabricPlatform;
import com.arcanc.pulselib.fabric.FabricPulsePluginFinder;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.plugin.IPulsePluginFinder;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.util.PLibDatabase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.util.List;

/** Verifies the real Fabric bootstrap and scoped registration before normal shutdown. */
public final class FabricBootstrapProbe implements ModInitializer
{
	public static int common;
	public static int server;
	public static int channels;
	public static int events;
	public static int deformers;

	@Override
	public void onInitialize()
	{
		ServerLifecycleEvents.SERVER_STARTED.register(minecraft ->
		{
			check(common == 1 && server == 1 && channels == 1 && events == 1 && deformers == 1, "production bootstrap completes registration before probe");
			check(PulseServices.PLATFORM instanceof FabricPlatform && !PulseServices.PLATFORM.isClient(), "Fabric physical-server service");
			IPulsePluginFinder finder = PulseServices.load(IPulsePluginFinder.class);
			check(finder instanceof FabricPulsePluginFinder, "Fabric scanner provider");
			check(finder.findPlugins().size() == 3 && finder.findPlugins().stream().allMatch(candidate ->
					candidate.modId().equals("pulselib_stage61")), "three external candidates including client poison");
			var manager = PulsePluginManager.get();
			check(manager.pluginIds().equals(List.of(PLibDatabase.rl("stage61/common"), PLibDatabase.rl("stage61/server"))), "side-filtered deterministic plugin IDs");
			manager.registerCommon();
			manager.registerCommon();
			check(common == 1 && server == 1 && channels == 1 && events == 1 && deformers == 1, "exactly-once common phases");
			check(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(PLibDatabase.rl("stage61/channel")).isPresent(), "plugin channel applied to real registry");
			PLibDatabase.LOGGER.info("STAGE61A FABRIC BOOTSTRAP PASS: candidates=3, common=1, server=1, client=0, channels=1, events=1, deformers=1, services=Fabric");
			minecraft.halt(false);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(minecraft -> PLibDatabase.LOGGER.info("STAGE61A FABRIC SHUTDOWN PASS"));
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE61A: " + message);
	}
}
