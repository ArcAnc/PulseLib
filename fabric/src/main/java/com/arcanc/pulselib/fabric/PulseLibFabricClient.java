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
import com.arcanc.pulselib.content.animatable.PLibAnimationTicker;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.content.model.textures.atlas.RuntimeLoader;
import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.arcanc.pulselib.content.renderer.PModelGuiRenderer;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PLibArmorHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.AtlasRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.SpriteSourceRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.server.packs.PackType;
import net.minecraft.client.resources.model.sprite.AtlasManager;

import java.util.Set;

/** Fabric client registration and shared model-reload lifecycle on the 26.1 foundation. */
public final class PulseLibFabricClient implements ClientModInitializer
{
	/**
	 * Registers canonical client content and shared model reload before resource preparation.
	 */
	@Override
	public void onInitializeClient()
	{
		PRenderTypes.register(RenderPipelines::register);
		PictureInPictureRendererRegistry.register(context -> new PModelGuiRenderer(context.bufferSource()));
		LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> PRenderStagesHandler.afterOpaqueFeatures());
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> PRenderStagesHandler.afterNativeFeatures());
		LevelRenderEvents.END_MAIN.register(context -> PRenderStagesHandler.afterParticles());
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> PLibArmorHandler.addLayers(renderer));
		PAttachmentAnchorResolvers.init();
		PulsePluginManager.get().registerClient();
		SpriteSourceRegistry.register(PLibDatabase.rl("runtime_loader"), RuntimeLoader.CODEC);
		AtlasRegistry.register(new AtlasManager.AtlasConfig(PResourceCache.ATLAS_LOCATION,
				PResourceCache.ATLAS_FILE_LOCATION, false, Set.of(PLibSpriteMetadata.TYPE)));
		ResourceLoader resources = ResourceLoader.get(PackType.CLIENT_RESOURCES);
		resources.registerReloadListener(PLibDatabase.RELOAD_LISTENER_ID, PModelCache::reload);
		resources.addListenerOrdering(ResourceReloaderKeys.Client.MODELS, PLibDatabase.RELOAD_LISTENER_ID);
		resources.addListenerOrdering(ResourceReloaderKeys.Client.ATLAS, PLibDatabase.RELOAD_LISTENER_ID);
		ClientTickEvents.START_LEVEL_TICK.register(PLibAnimationTicker::clientTick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> PLibAnimationTicker.afterClientTick());
		ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> InstanceAnimationManager.removeManager(entity));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(PulseRuntime::disconnected));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PulseRuntime.disconnected());
	}
}
