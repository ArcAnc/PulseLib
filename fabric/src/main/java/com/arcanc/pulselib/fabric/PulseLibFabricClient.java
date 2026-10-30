/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.content.event.PulseRuntime;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.server.packs.PackType;

/** Fabric client registration and shared model-reload lifecycle on the 26.1 foundation. */
public final class PulseLibFabricClient implements ClientModInitializer
{
	/**
	 * Registers canonical client content and shared model reload before resource preparation.
	 */
	@Override
	public void onInitializeClient()
	{
		PAttachmentAnchorResolvers.init();
		PulsePluginManager.get().registerClient();
		ResourceLoader resources = ResourceLoader.get(PackType.CLIENT_RESOURCES);
		resources.registerReloadListener(PLibDatabase.RELOAD_LISTENER_ID, PModelCache::reload);
		resources.addListenerOrdering(ResourceReloaderKeys.Client.MODELS, PLibDatabase.RELOAD_LISTENER_ID);
		resources.addListenerOrdering(ResourceReloaderKeys.Client.ATLAS, PLibDatabase.RELOAD_LISTENER_ID);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PulseRuntime.disconnected());
	}
}
