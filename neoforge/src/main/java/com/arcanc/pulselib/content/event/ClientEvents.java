package com.arcanc.pulselib.content.event;

import com.arcanc.pulselib.content.animatable.PItemAnimatable;
import com.arcanc.pulselib.content.model.textures.atlas.RuntimeLoader;
import com.arcanc.pulselib.neoforge.NeoForgeClientLifecycle;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorClientExtensions;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

public class ClientEvents
{
	private static boolean pulseClientContentRegistered;

	public static void registerClientEvents(IEventBus bus)
	{
		bus.addListener(EventPriority.HIGHEST, ClientEvents::registerSpriteSources);
		bus.addListener(ClientEvents::registerReloadListeners);
		bus.addListener(ClientEvents::clientSetup);
		bus.addListener(ClientEvents::registerClientExtensions);
		NeoForge.EVENT_BUS.addListener(ClientEvents::playerDisconnected);
		NeoForgeClientLifecycle.register(bus);
	}

	private static void clientSetup(FMLClientSetupEvent event)
	{
		event.enqueueWork(ClientEvents::ensurePulseClientContentRegistered);
	}

	public static synchronized void ensurePulseClientContentRegistered()
	{
		if (pulseClientContentRegistered)
			return;
		PAttachmentAnchorResolvers.init();
		PulsePluginManager.get().registerCommon();
		PulseLibEvents.TypeRegistrationEvent types = new PulseLibEvents.TypeRegistrationEvent();
		ModLoader.postEvent(types);
		types.apply();
		PulsePluginManager.get().registerClient();
		PulseLibEvents.AttachmentRegistrationEvent attachments = new PulseLibEvents.AttachmentRegistrationEvent();
		ModLoader.postEvent(attachments);
		attachments.registration().apply();
		PulseLibEvents.PlayerAnimationRegistrationEvent animations = new PulseLibEvents.PlayerAnimationRegistrationEvent();
		ModLoader.postEvent(animations);
		animations.registration().apply();
		PulseLibEvents.PlayerAnimatedAttachmentRegistrationEvent animated = new PulseLibEvents.PlayerAnimatedAttachmentRegistrationEvent();
		ModLoader.postEvent(animated);
		animated.registration().apply();
		pulseClientContentRegistered = true;
	}

	private static void registerClientExtensions(RegisterClientExtensionsEvent event)
	{
		ensurePulseClientContentRegistered();
		BuiltInRegistries.ITEM.stream().filter(item -> item instanceof PItemAnimatable<?> || item instanceof PArmorModelProvider || PLivingAttachments.contains(item)).forEach(item -> {
			if (event.isItemRegistered(item))
				return;
			IClientItemExtensions base = item instanceof PItemAnimatable<?> animatable ? animatable.registerClientExtension() : IClientItemExtensions.DEFAULT;
			IClientItemExtensions extension = PArmorClientExtensions.buildFor(item, base);
			if (extension != IClientItemExtensions.DEFAULT)
				event.registerItem(extension, item);
		});
	}

	private static void registerReloadListeners(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener(PModelCache::reload);
	}

	private static void playerDisconnected(LevelEvent.Unload event)
	{
		if (event.getLevel().isClientSide())
			PulseRuntime.disconnected();
	}

	private static void registerSpriteSources(RegisterSpriteSourceTypesEvent event)
	{
		event.register(PLibDatabase.rl("runtime_loader"), RuntimeLoader.TYPE);
	}
}
