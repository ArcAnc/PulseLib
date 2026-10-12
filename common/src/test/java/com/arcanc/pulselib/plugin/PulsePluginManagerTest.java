package com.arcanc.pulselib.plugin;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import org.joml.Vector3f;
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

	@Test
	void finderFailureIsTerminalWithoutRescanning()
	{
		AtomicInteger scans = new AtomicInteger();
		var failure = new IllegalArgumentException("Broken scan metadata");
		var manager = new PulsePluginManager(() -> { scans.incrementAndGet(); throw failure; }, false, getClass().getClassLoader());
		assertSame(failure, assertThrows(IllegalArgumentException.class, manager::discover));
		assertSame(failure, assertThrows(IllegalStateException.class, manager::pluginIds).getCause());
		assertEquals(1, scans.get());
	}

	@Test
	void internalConstructorFailureNeverPublishesOrRecreatesPartialInstances()
	{
		ClientPlugin.constructions = 0;
		AtomicInteger scans = new AtomicInteger();
		var manager = new PulsePluginManager(() -> {
			scans.incrementAndGet();
			return List.of(candidate(ClientPlugin.class, PulsePlugin.Side.CLIENT),
					new PulsePluginCandidate(ZBrokenPlugin.class.getName(), "pulselib", PulsePlugin.Side.BOTH));
		}, true, getClass().getClassLoader());
		assertThrows(IllegalStateException.class, manager::discover);
		assertThrows(IllegalStateException.class, manager::pluginIds);
		assertEquals(1, scans.get());
		assertEquals(1, ClientPlugin.constructions);
	}

	@Test
	void recursiveConstructorIsIsolatedWithoutAnotherScan()
	{
		AtomicInteger scans = new AtomicInteger();
		var manager = new PulsePluginManager(() -> {
			scans.incrementAndGet();
			return List.of(candidate(RecursivePlugin.class, PulsePlugin.Side.BOTH), candidate(CommonPlugin.class, PulsePlugin.Side.BOTH));
		}, false, getClass().getClassLoader());
		RecursivePlugin.callback = manager::discover;
		assertEquals(List.of(ResourceLocation.fromNamespaceAndPath("test", "common")), manager.pluginIds());
		assertEquals(1, scans.get());
		RecursivePlugin.callback = null;
	}

	@Test
	void invalidIdsAreIsolated()
	{
		var manager = new PulsePluginManager(() -> List.of(candidate(NullIdPlugin.class, PulsePlugin.Side.BOTH),
				candidate(InvalidIdPlugin.class, PulsePlugin.Side.BOTH), candidate(CommonPlugin.class, PulsePlugin.Side.BOTH)), false, getClass().getClassLoader());
		assertEquals(List.of(ResourceLocation.fromNamespaceAndPath("test", "common")), manager.pluginIds());
	}

	@Test
	void externalRegistrationFailureDiscardsItsBatchAndRunsOtherPluginsOnce()
	{
		FailingRegistration.calls = 0;
		var manager = new PulsePluginManager(() -> List.of(candidate(FailingRegistration.class, PulsePlugin.Side.BOTH),
				candidate(CommonPlugin.class, PulsePlugin.Side.BOTH)), false, getClass().getClassLoader());
		manager.registerCommon();
		manager.registerCommon();
		assertEquals(1, FailingRegistration.calls);
		assertTrue(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(FailingRegistration.ID).isEmpty());
	}

	@Test
	void failedInternalOneTimePhaseCannotBeRepeatedOrReportedComplete()
	{
		FailingRegistration.calls = 0;
		var manager = new PulsePluginManager(() -> List.of(new PulsePluginCandidate(FailingRegistration.class.getName(), "pulselib", PulsePlugin.Side.BOTH)), false, getClass().getClassLoader());
		assertThrows(IllegalStateException.class, manager::registerCommon);
		var repeat = assertThrows(IllegalStateException.class, manager::registerCommon);
		assertTrue(repeat.getMessage().contains("CHANNELS failed"));
		assertEquals(1, FailingRegistration.calls);
		assertTrue(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(FailingRegistration.ID).isEmpty());
	}

	@Test
	void failedModelsCanRetryWithoutRecreatingPluginsOrPublishingPartialDeclarations()
	{
		FailingModels.calls = 0;
		FailingModels.constructions = 0;
		var manager = new PulsePluginManager(() -> List.of(new PulsePluginCandidate(FailingModels.class.getName(), "pulselib", PulsePlugin.Side.CLIENT)), true, getClass().getClassLoader());
		var resources = new HashMap<ResourceLocation, PModelResource>();
		assertThrows(IllegalStateException.class, () -> manager.registerModels(resources));
		assertTrue(resources.isEmpty());
		manager.registerModels(resources);
		assertTrue(resources.containsKey(FailingModels.ID));
		assertEquals(2, FailingModels.calls);
		assertEquals(1, FailingModels.constructions);
	}

	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static class RecursivePlugin extends CommonPlugin
	{
		static Runnable callback;
		public RecursivePlugin() { callback.run(); }
	}

	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static class NullIdPlugin extends CommonPlugin
	{
		@Override public ResourceLocation getPluginId() { return null; }
	}

	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static class InvalidIdPlugin extends CommonPlugin
	{
		@Override public ResourceLocation getPluginId() { return ResourceLocation.fromNamespaceAndPath("INVALID", "path"); }
	}

	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static class FailingRegistration extends CommonPlugin
	{
		static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("test", "discarded_channel");
		static int calls;
		@Override public ResourceLocation getPluginId() { return ID; }
		@Override public void registerAnimationChannels(PTypeRegistrar registrar)
		{
			calls++;
			registrar.registerAnimationChannel(new PAnimationChannel.Vector3fChannelType(ID, new Vector3f(), false));
			throw new IllegalStateException("Registration aborted before commit");
		}
	}

	@PulsePlugin
	public static class FailingModels implements IPulseClientPlugin
	{
		static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("test", "retry_model");
		static int calls;
		static int constructions;
		public FailingModels() { constructions++; }
		@Override public ResourceLocation getPluginId() { return ID; }
		@Override public void registerModels(PModelRegistrar registrar)
		{
			registrar.model(ID).texture("0", ID);
			if (++calls == 1)
				throw new IllegalStateException("First reload failed");
		}
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
	@PulsePlugin(PulsePlugin.Side.BOTH)
	public static class ZBrokenPlugin extends BrokenPlugin { }

}
