package com.arcanc.pulselib.plugin;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class PulsePluginManagerTest
{
	@BeforeAll
	static void bootstrap()
	{
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void serverNeverRequestsClientPluginClass()
	{
		AtomicInteger loads = new AtomicInteger();
		ClassLoader loader = new ClassLoader(getClass().getClassLoader())
		{
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
			{
				if (name.equals(ClientPlugin.class.getName()))
				{
					loads.incrementAndGet();
					throw new ClassNotFoundException("Client classes forbidden");
				}
				return super.loadClass(name, resolve);
			}
		};
		var manager = new PulsePluginManager(() -> List.of(candidate(ClientPlugin.class, PulsePlugin.Side.CLIENT)), false, loader);
		assertTrue(manager.pluginIds().isEmpty());
		assertEquals(0, loads.get());
	}

	@Test
	void deduplicatesClassesAndRetainsInstanceAcrossModelReloads()
	{
		ClientPlugin.constructions = 0;
		ClientPlugin.models = 0;
		ClientPlugin.attachments = 0;
		AtomicInteger scans = new AtomicInteger();
		var candidate = candidate(ClientPlugin.class, PulsePlugin.Side.CLIENT);
		var manager = new PulsePluginManager(() -> { scans.incrementAndGet(); return List.of(candidate, candidate); },
				true, getClass().getClassLoader());
		manager.registerModels(new HashMap<>());
		manager.registerModels(new HashMap<>());
		assertEquals(1, scans.get());
		assertEquals(1, ClientPlugin.constructions);
		assertEquals(1, ClientPlugin.attachments);
		assertEquals(2, ClientPlugin.models);
	}

	@Test
	void isolatesThirdPartyFailuresAndDuplicateIds()
	{
		var manager = new PulsePluginManager(() -> List.of(candidate(BrokenPlugin.class, PulsePlugin.Side.BOTH),
				candidate(CommonPlugin.class, PulsePlugin.Side.BOTH), candidate(DuplicatePlugin.class, PulsePlugin.Side.BOTH)),
				false, getClass().getClassLoader());
		assertEquals(List.of(ResourceLocation.fromNamespaceAndPath("test", "common")), manager.pluginIds());
	}

	@Test
	void failsForBrokenInternalComponent()
	{
		var manager = new PulsePluginManager(() -> List.of(new PulsePluginCandidate(BrokenPlugin.class.getName(), "pulselib", PulsePlugin.Side.BOTH)),
				false, getClass().getClassLoader());
		assertThrows(IllegalStateException.class, manager::discover);
	}

	private static PulsePluginCandidate candidate(Class<?> type, PulsePlugin.Side side)
	{
		return new PulsePluginCandidate(type.getName(), "test", side);
	}

	@PulsePlugin
	public static class ClientPlugin implements IPulseClientPlugin
	{
		static int constructions;
		static int models;
		static int attachments;
		public ClientPlugin() { constructions++; }
		@Override public ResourceLocation getPluginId() { return ResourceLocation.fromNamespaceAndPath("test", "client"); }
		@Override public void registerModels(PModelRegistrar registrar) { models++; }
		@Override public void registerAttachments(PAttachmentRegistrar registrar) { attachments++; }
	}

	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static class CommonPlugin implements IPulseCommonPlugin
	{
		@Override public ResourceLocation getPluginId() { return ResourceLocation.fromNamespaceAndPath("test", "common"); }
	}

	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static class DuplicatePlugin extends CommonPlugin { }

	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static class BrokenPlugin implements IPulseCommonPlugin
	{
		public BrokenPlugin() { throw new IllegalStateException("Test failure"); }
		@Override public ResourceLocation getPluginId() { throw new AssertionError(); }
	}
}
