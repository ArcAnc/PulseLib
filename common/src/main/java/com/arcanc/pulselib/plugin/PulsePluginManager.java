package com.arcanc.pulselib.plugin;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.*;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

public final class PulsePluginManager
{
	private final IPulsePluginFinder finder;
	private final boolean client;
	private final ClassLoader classLoader;
	private final PulsePluginLifecycle lifecycle = new PulsePluginLifecycle();
	private List<LoadedPlugin> plugins;
	private boolean discovering;

	public PulsePluginManager(IPulsePluginFinder finder, boolean client, ClassLoader classLoader)
	{
		this.finder = Objects.requireNonNull(finder);
		this.client = client;
		this.classLoader = Objects.requireNonNull(classLoader);
	}

	private static final class Holder
	{
		private static final PulsePluginManager INSTANCE = new PulsePluginManager(
				PulseServices.load(IPulsePluginFinder.class), PulseServices.PLATFORM.isClient(), PulsePluginManager.class.getClassLoader());
	}

	public static PulsePluginManager get()
	{
		return Holder.INSTANCE;
	}

	public synchronized void discover()
	{
		if (this.plugins != null)
			return;
		if (this.discovering)
			throw new IllegalStateException("Plugin discovery cannot be called from a plugin constructor");
		this.discovering = true;
		long start = System.nanoTime();
		Map<String, PulsePluginCandidate> candidates = new TreeMap<>();
		this.finder.findPlugins().stream().sorted(Comparator.comparing(PulsePluginCandidate::modId)
				.thenComparing(PulsePluginCandidate::className)).forEach(candidate -> candidates.putIfAbsent(candidate.className(), candidate));
		Map<ResourceLocation, LoadedPlugin> ids = new TreeMap<>();
		for (PulsePluginCandidate candidate : candidates.values())
		{
			// This check MUST precede class loading, including loading interfaces and constructors.
			if (!candidate.side().matches(this.client))
				continue;
			try
			{
				Class<?> type = Class.forName(candidate.className(), false, this.classLoader);
				PulsePlugin annotation = type.getAnnotation(PulsePlugin.class);
				if (annotation == null || annotation.value() != candidate.side())
					throw new IllegalArgumentException("Plugin annotation disagrees with discovery metadata");
				if (!Modifier.isPublic(type.getModifiers()) || Modifier.isAbstract(type.getModifiers()) || type.isInterface())
					throw new IllegalArgumentException("Plugin must be a public concrete class");
				if (!IPulseCommonPlugin.class.isAssignableFrom(type))
					throw new IllegalArgumentException("Plugin must implement IPulseCommonPlugin or IPulseClientPlugin");
				boolean clientPlugin = IPulseClientPlugin.class.isAssignableFrom(type);
				if ((candidate.side() == PulsePlugin.Side.CLIENT) != clientPlugin)
					throw new IllegalArgumentException("CLIENT requires IPulseClientPlugin; SERVER/BOTH require IPulseCommonPlugin without IPulseClientPlugin");
				IPulseCommonPlugin instance = (IPulseCommonPlugin) type.getConstructor().newInstance();
				ResourceLocation id = Objects.requireNonNull(instance.getPluginId(), "plugin id");
				LoadedPlugin plugin = new LoadedPlugin(candidate, id, instance);
				LoadedPlugin previous = ids.putIfAbsent(id, plugin);
				if (previous != null)
					throw new IllegalArgumentException("Duplicate plugin id " + id + "; already used by " + previous.candidate);
			}
			catch (Throwable exception)
			{
				failure(candidate, "discovery", exception);
			}
		}
		this.plugins = List.copyOf(ids.values());
		this.discovering = false;
		PLibDatabase.LOGGER.info("PulseLib discovered {} plugin(s) from {} candidate(s) in {} ms",
				this.plugins.size(), candidates.size(), (System.nanoTime() - start) / 1_000_000);
	}

	public synchronized List<ResourceLocation> pluginIds()
	{
		discover();
		return this.plugins.stream().map(plugin -> plugin.id).toList();
	}

	public synchronized void registerCommon()
	{
		discover();
		phase(PulsePluginLifecycle.Phase.CHANNELS, plugin -> {
			try (PTypeRegistrar registrar = new PTypeRegistrar(PTypeRegistrar.Kind.CHANNELS))
			{
				plugin.instance.registerAnimationChannels(registrar);
				registrar.apply();
			}
		});
		phase(PulsePluginLifecycle.Phase.EVENTS, plugin -> {
			try (PTypeRegistrar registrar = new PTypeRegistrar(PTypeRegistrar.Kind.EVENTS))
			{
				plugin.instance.registerAnimationEvents(registrar);
				registrar.apply();
			}
		});
		phase(PulsePluginLifecycle.Phase.DEFORMERS, plugin -> {
			try (PTypeRegistrar registrar = new PTypeRegistrar(PTypeRegistrar.Kind.DEFORMERS))
			{
				plugin.instance.registerDeformers(registrar);
				registrar.apply();
			}
		});
	}

	public synchronized void registerClient()
	{
		if (!this.client)
			throw new IllegalStateException("Client registration attempted on dedicated server");
		registerCommon();
		phase(PulsePluginLifecycle.Phase.ATTACHMENTS, plugin -> {
			if (plugin.instance instanceof IPulseClientPlugin extension)
				try (PAttachmentRegistrar registrar = new PAttachmentRegistrar())
				{
					extension.registerAttachments(registrar);
					registrar.apply();
				}
		});
		phase(PulsePluginLifecycle.Phase.PLAYER_ANIMATIONS, plugin -> {
			if (plugin.instance instanceof IPulseClientPlugin extension)
				try (PPlayerAnimationRegistrar registrar = new PPlayerAnimationRegistrar())
				{
					extension.registerPlayerAnimations(registrar);
					registrar.apply();
				}
		});
		phase(PulsePluginLifecycle.Phase.ANIMATED_ATTACHMENTS, plugin -> {
			if (plugin.instance instanceof IPulseClientPlugin extension)
				try (PAnimatedAttachmentRegistrar registrar = new PAnimatedAttachmentRegistrar())
				{
					extension.registerAnimatedAttachments(registrar);
					registrar.apply();
				}
		});
	}

	public synchronized void registerModels(Map<ResourceLocation, PModelResource> resources)
	{
		registerClient();
		phase(PulsePluginLifecycle.Phase.MODELS, plugin -> {
			if (plugin.instance instanceof IPulseClientPlugin extension)
				try (PModelRegistrar registrar = new PModelRegistrar())
				{
					extension.registerModels(registrar);
					registrar.apply(resources);
				}
		});
	}

	private void phase(PulsePluginLifecycle.Phase phase, Consumer<LoadedPlugin> action)
	{
		if (!this.lifecycle.begin(phase))
			return;
		for (LoadedPlugin plugin : this.plugins)
		{
			if (plugin.disabled)
				continue;
			try
			{
				action.accept(plugin);
			}
			catch (Throwable exception)
			{
				if (phase != PulsePluginLifecycle.Phase.MODELS)
					plugin.disabled = true;
				failure(plugin.candidate, phase.name(), exception);
			}
		}
		this.lifecycle.complete(phase);
	}

	private static void failure(PulsePluginCandidate candidate, String phase, Throwable exception)
	{
		Throwable cause = exception instanceof java.lang.reflect.InvocationTargetException invocation ? invocation.getCause() : exception;
		if (cause instanceof VirtualMachineError fatal)
			throw fatal;
		if (cause instanceof ThreadDeath fatal)
			throw fatal;
		if (Arrays.asList(candidate.modId().split(",")).contains(PLibDatabase.MOD_ID))
			throw new IllegalStateException("Internal PulseLib plugin failed during " + phase + ": " + candidate.className(), cause);
		PLibDatabase.LOGGER.error("PulseLib plugin {} from mod {} failed during {}", candidate.className(), candidate.modId(), phase, cause);
	}

	private static final class LoadedPlugin
	{
		private final PulsePluginCandidate candidate;
		private final ResourceLocation id;
		private final IPulseCommonPlugin instance;
		private boolean disabled;

		private LoadedPlugin(PulsePluginCandidate candidate, ResourceLocation id, IPulseCommonPlugin instance)
		{
			this.candidate = candidate;
			this.id = id;
			this.instance = instance;
		}
	}
}
