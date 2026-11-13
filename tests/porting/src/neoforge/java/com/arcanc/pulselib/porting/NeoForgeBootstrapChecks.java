/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting;

import com.arcanc.pulselib.PLib;
import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.plugin.IPulsePluginFinder;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.server.loading.ServerModLoader;
import org.joml.Vector3f;

/** Validates actual mod construction, annotation scan discovery and the deferred common setup queue. */
@Mod("pulselib_porting_fixture")
public final class NeoForgeBootstrapChecks
{
	private static int registrations;
	private static boolean dedicatedSetup;
	private static boolean loadComplete;

	public NeoForgeBootstrapChecks(IEventBus bus)
	{
		bus.addListener((FMLDedicatedServerSetupEvent event) -> dedicatedSetup = true);
		bus.addListener((FMLLoadCompleteEvent event) -> loadComplete = true);
	}

	public static void main(String[] args)
	{
		SharedConstants.tryDetectVersion();
		check(SharedConstants.getCurrentVersion().id().equals("26.2"), "Actual Minecraft 26.2");
		Bootstrap.bootStrap();
		Bootstrap.validate();
		ServerModLoader.load(false);
		check(!ServerModLoader.hasErrors(), "Real server mod loader succeeded");
		check(ModList.get().getModContainerById("pulselib").isPresent() &&
				PLib.class.getClassLoader() == NeoForgeBootstrapChecks.class.getClassLoader(), "Actual PulseLib mod/content loader");
		check(!PulseServices.PLATFORM.isClient(), "Actual dedicated NeoForge platform");
		check(dedicatedSetup && loadComplete && registrations == 1, "FML lifecycle and real enqueueWork callback");
		check(PulsePluginManager.get().pluginIds().equals(java.util.List.of(id("plugin"))), "Real manager registration");
		check(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(id("channel")).isPresent(), "Applied channel registration");
		var candidates = PulseServices.load(IPulsePluginFinder.class).findPlugins();
		check(candidates.stream().anyMatch(candidate -> candidate.side() == PulsePlugin.Side.CLIENT &&
				candidate.className().endsWith("ForbiddenNeoForgeClientPlugin")), "Real client scan metadata");
		PulsePluginManager.get().registerCommon();
		check(registrations == 1, "Common registration executes once");
		PLibDatabase.LOGGER.info("PASS actual PLib constructor, FML common/dedicated/load-complete lifecycle and enqueueWork");
		PLibDatabase.LOGGER.info("PASS real NeoForge annotation scan, platform services, side filtering and plugin registration");
		PLibDatabase.LOGGER.info("PULSELIB_NEOFORGE_FIXTURE_PASSED; no MinecraftServer or world created.");
	}

	private static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath("porting_fixture", path);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	@PulsePlugin(PulsePlugin.Side.SERVER)
	public static final class FixturePlugin implements IPulseCommonPlugin
	{
		@Override
		public Identifier getPluginId()
		{
			return id("plugin");
		}

		@Override
		public void registerAnimationChannels(PTypeRegistrar registrar)
		{
			registrations++;
			registrar.registerAnimationChannel(new PAnimationChannel.Vector3fChannelType(id("channel"), new Vector3f(), false));
		}
	}
}
