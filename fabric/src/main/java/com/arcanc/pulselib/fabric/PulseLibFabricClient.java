/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric;

import com.arcanc.pulselib.content.animatable.PItemAnimatable;
import com.arcanc.pulselib.content.animatable.PLibAnimationTicker;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.event.PulseRuntime;
import com.arcanc.pulselib.content.model.textures.atlas.RuntimeLoader;
import com.arcanc.pulselib.fabric.mixin.SpriteSourcesAccessor;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PLibArmorHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fabric client entry point for shader, atlas, reload, attachment and animation lifecycle integration.
 */
public final class PulseLibFabricClient implements ClientModInitializer
{
	/**
	 * Installs Fabric client registrations, reload listeners, render hooks and animation lifecycle callbacks.
	 */
	@Override
	public void onInitializeClient()
	{
		PAttachmentAnchorResolvers.init();
		PulsePluginManager.get().registerClient();
		SpriteSourcesAccessor.pulselib$getTypes().put(PLibDatabase.rl("runtime_loader"), RuntimeLoader.TYPE);
		CoreShaderRegistrationCallback.EVENT.register(context -> PRenderTypes.ShadersProvider.registerShaders(context :: register));
		ClientTickEvents.START_WORLD_TICK.register(PLibAnimationTicker :: clientTick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> PLibAnimationTicker.afterClientTick());
		ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> InstanceAnimationManager.removeManager(entity));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(PulseRuntime :: disconnected));
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> PLibArmorHandler.addLayers(renderer));
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ModelReloadListener());
		BuiltInRegistries.ITEM.forEach(item ->
		{
			if (item instanceof PItemAnimatable<?> animatable)
			{
				var renderer = animatable.getPulseItemRenderer();
				if (renderer != null)
					BuiltinItemRendererRegistry.INSTANCE.register(item, renderer :: renderByItem);
			}
		});
	}

	/**
	 * Fabric resource reload adapter that loads PulseLib models after the required atlas resources.
	 */
	private static final class ModelReloadListener implements IdentifiableResourceReloadListener
	{
		/**
		 * Returns the stable identifier of the PulseLib model reload listener.
		 *
		 * @return the stable identifier of the PulseLib model reload listener
		 */
		@Override
		public ResourceLocation getFabricId()
		{
			return PLibDatabase.rl("models");
		}

		/**
		 * Returns reload listener dependencies required before models can be baked.
		 *
		 * @return reload listener dependencies required before models can be baked
		 */
		@Override
		public Collection<ResourceLocation> getFabricDependencies()
		{
			return List.of(ResourceReloadListenerKeys.MODELS, ResourceReloadListenerKeys.TEXTURES);
		}

		/**
		 * Delegates model preparation and application to the shared model cache.
		 *
		 * @param barrier the barrier separating reload preparation from application
		 * @param manager the manager
		 * @param prepareProfiler the profiler for reload preparation
		 * @param applyProfiler the profiler for applying prepared reload data
		 * @param prepareExecutor the executor used for background reload preparation
		 * @param applyExecutor the executor used to apply prepared reload results
		 * @return a future completing when PulseLib models have been published
		 */
		@Override
		public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
		                                      ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
		                                      Executor prepareExecutor, Executor applyExecutor)
		{
			return PModelCache.reload(barrier, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor);
		}
	}
}
