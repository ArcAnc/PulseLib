/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;


import com.arcanc.pulselib.neoforge.NeoForgeClientLifecycle;
import com.arcanc.pulselib.content.model.textures.atlas.RuntimeLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorClientExtensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Provides support for client events.
 */
public class ClientEvents
{
	private static boolean pulseClientContentRegistered;
	
	/**
	 * Registers the client events.
	 * @param modEventBus the mod event bus to use.
	 */
	public static void registerClientEvents(final IEventBus modEventBus)
	{
		ModLoader.postEvent(new PulseLibEvents.TypeRegistrationEvent());
		PResourceCache.setResourceRegistrar(resources ->
		{
			PulseLibEvents.RegisterResourceEvent event = new PulseLibEvents.RegisterResourceEvent();
			ModLoader.postEvent(event);
			event.apply(resources);
		});
		NeoForgeClientLifecycle.register(modEventBus);

		PAttachmentAnchorResolvers.init();
		modEventBus.addListener(EventPriority.HIGHEST, ClientEvents :: registerSpriteSources);
		modEventBus.addListener(ClientEvents :: registerReloadListeners);
		modEventBus.addListener(ClientEvents :: registerPulseClientContent);
		modEventBus.addListener(ClientEvents :: registerClientExtensions);
		NeoForge.EVENT_BUS.addListener(ClientEvents :: playerDisconnected);
		
	}
	
	/**
	 * Registers the reload listeners.
	 * @param event the event to use.
	 */
	private static void registerReloadListeners(final AddClientReloadListenersEvent event)
	{
		PulseRuntime.beforeResourceReload();
		event.addListener(PLibDatabase.RELOAD_LISTENER_ID, PModelCache :: reload);
	}
	
	/**
	 * Registers the pulse client content.
	 * @param event the event to use.
	 */
	private static void registerPulseClientContent(final FMLClientSetupEvent event)
	{
		ensurePulseClientContentRegistered();
	}
	
	/**
	 * Performs the ensure pulse client content registered operation.
	 */
	private static void ensurePulseClientContentRegistered()
	{
		if (pulseClientContentRegistered)
			return;
		
		PulseLibEvents.AttachmentRegistrationEvent registrationEvent = new PulseLibEvents.AttachmentRegistrationEvent();
		ModLoader.postEvent(registrationEvent);
		PulseLibEvents.PlayerAnimationRegistrationEvent playerAnimationRegistrationEvent = new PulseLibEvents.PlayerAnimationRegistrationEvent();
		ModLoader.postEvent(playerAnimationRegistrationEvent);
		PulseLibEvents.PlayerAnimatedAttachmentRegistrationEvent animatedAttachmentRegistrationEvent = new PulseLibEvents.PlayerAnimatedAttachmentRegistrationEvent();
		ModLoader.postEvent(animatedAttachmentRegistrationEvent);

		registrationEvent.registration().apply();
		playerAnimationRegistrationEvent.registration().apply();
		animatedAttachmentRegistrationEvent.registration().apply();
		pulseClientContentRegistered = true;
	}
	
	/**
	 * Registers the client extensions.
	 * @param event the event to use.
	 */
	private static void registerClientExtensions(final RegisterClientExtensionsEvent event)
	{
		ensurePulseClientContentRegistered();
		
		BuiltInRegistries.ITEM.stream().
				filter(PLivingAttachments :: contains).
				forEach(item -> registerClientExtensionWithItem(event, item));
	}
	
	/**
	 * Registers the client extension with item.
	 * @param event the event to use.
	 * @param item the item to use.
	 */
	private static void registerClientExtensionWithItem(final RegisterClientExtensionsEvent event, final Item item)
	{
		if (event.isItemRegistered(item))
			return;
		
		IClientItemExtensions extension = PArmorClientExtensions.buildFor(item);
		
		if (extension != IClientItemExtensions.DEFAULT)
			event.registerItem(extension, item);
	}
	
	/**
	 * Performs the player disconnected operation.
	 * @param event the event to use.
	 */
	private static void playerDisconnected(final LevelEvent.Unload event)
	{
		if (!event.getLevel().isClientSide())
			return;
		PulseRuntime.disconnected();
	}
	
	/**
	 * Registers the sprite sources.
	 * @param event the event to use.
	 */
	private static void registerSpriteSources(final RegisterSpriteSourcesEvent event)
	{
		event.register(PLibDatabase.rl("runtime_loader"), RuntimeLoader.CODEC);
	}
	
}
