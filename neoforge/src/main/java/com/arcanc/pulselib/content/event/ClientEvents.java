/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

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
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * NeoForge client mod-bus registrations for plugin content, atlas sources, resource reloads and item
 * extensions.
 */
public class ClientEvents
{
	private static boolean pulseClientContentRegistered;

	/**
	 * Installs NeoForge client lifecycle and registration event listeners.
	 *
	 * @param bus the NeoForge mod lifecycle event bus
	 */
	public static void registerClientEvents(IEventBus bus)
	{
		bus.addListener(EventPriority.HIGHEST, ClientEvents :: registerSpriteSources);
		bus.addListener(ClientEvents :: registerReloadListeners);
		bus.addListener(ClientEvents :: clientSetup);
		bus.addListener(ClientEvents :: registerClientExtensions);
		NeoForge.EVENT_BUS.addListener(ClientEvents :: playerDisconnected);
		NeoForgeClientLifecycle.register(bus);
	}

	private static void clientSetup(FMLClientSetupEvent event)
	{
		event.enqueueWork(ClientEvents :: ensurePulseClientContentRegistered);
	}

	/**
	 * Registers built-in anchors and client plugin content once before consumers require it.
	 */
	public static synchronized void ensurePulseClientContentRegistered()
	{
		if (pulseClientContentRegistered)
			return;
		PAttachmentAnchorResolvers.init();
		PulsePluginManager.get().registerClient();
		pulseClientContentRegistered = true;
	}

	private static void registerClientExtensions(RegisterClientExtensionsEvent event)
	{
		ensurePulseClientContentRegistered();
		BuiltInRegistries.ITEM.stream().
				filter(item -> item instanceof PItemAnimatable<?> || item instanceof PArmorModelProvider || PLivingAttachments.contains(item)).
				forEach(item ->
			{
				if (event.isItemRegistered(item))
					return;
				IClientItemExtensions base = IClientItemExtensions.DEFAULT;
				if (item instanceof PItemAnimatable<?> animatable)
				{
					var renderer = animatable.getPulseItemRenderer();
					if (renderer != null)
						base = new IClientItemExtensions()
						{
							@Override
							public BlockEntityWithoutLevelRenderer getCustomRenderer()
							{
								return renderer;
							}
						};
				}
				IClientItemExtensions extension = PArmorClientExtensions.buildFor(item, base);
				if (extension != IClientItemExtensions.DEFAULT)
					event.registerItem(extension, item);
			});
	}

	private static void registerReloadListeners(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener(PModelCache :: reload);
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
