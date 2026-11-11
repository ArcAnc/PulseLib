/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.content.animatable.PLibAnimationTicker;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.event.PulseRuntime;
import com.arcanc.pulselib.content.model.textures.atlas.PLibMetadata;
import com.arcanc.pulselib.content.model.textures.atlas.PLibSpriteMetadata;
import com.arcanc.pulselib.content.renderer.PModelGuiRenderer;
import com.arcanc.pulselib.content.renderer.PModelGuiRenderState;
import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.arcanc.pulselib.platform.PClientRenderHooks;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PLibArmorHandler;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** NeoForge event wiring for shared Minecraft 26.1 client operations. */
public final class NeoForgeClientLifecycle
{
	public NeoForgeClientLifecycle()
	{
	}

	public static void register(IEventBus bus)
	{
		PClientRenderHooks.installArmorModels(NeoForgeClientLifecycle::armorModel);
		PClientRenderHooks.install(TextureFormat::hasStencilAspect, (renderer, pose, collector, light, skin, sleeve, player, right) ->
		{
			if (right)
				renderer.renderRightHand(pose, collector, light, skin, sleeve, player);
			else
				renderer.renderLeftHand(pose, collector, light, skin, sleeve, player);
		});
		bus.addListener((RegisterRenderPipelinesEvent event) -> PRenderTypes.register(event::registerPipeline));
		bus.addListener(NeoForgeClientLifecycle::registerAtlases);
		bus.addListener((RegisterPictureInPictureRenderersEvent event) -> event.register(PModelGuiRenderState.class, PModelGuiRenderer::new));
		bus.addListener(NeoForgeClientLifecycle::addLayers);
		bus.addListener(NeoForgeClientLifecycle::registerRenderStateModifiers);
		NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre event) ->
		{
			if (event.getLevel() instanceof ClientLevel level)
				PLibAnimationTicker.clientTick(level);
		});
		NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> PulseRuntime.disconnected());
		NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) ->
		{
			if (event.getLevel() instanceof ClientLevel)
				InstanceAnimationManager.removeManager(event.getEntity());
		});
		NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload event) ->
		{
			if (event.getLevel() instanceof ClientLevel && event.getChunk() instanceof LevelChunk chunk)
				for (var block : chunk.getBlockEntities().values())
					InstanceAnimationManager.removeManager(block);
		});
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> PLibAnimationTicker.afterClientTick());
		NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterOpaqueFeatures event) -> PRenderStagesHandler.afterOpaqueFeatures());
		NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentFeatures event) -> PRenderStagesHandler.afterNativeFeatures());
		NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentParticles event) -> PRenderStagesHandler.afterParticles());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Model<?> armorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model<?> model)
	{
		return IClientItemExtensions.of(stack).getGenericArmorModel(stack, type, (Model)model);
	}

	private static void registerAtlases(RegisterTextureAtlasesEvent event)
	{
		event.register(new AtlasManager.AtlasConfig(PResourceCache.ATLAS_LOCATION, PResourceCache.ATLAS_FILE_LOCATION, false));
		event.addAdditionalMetadata(PResourceCache.ATLAS_FILE_LOCATION, PLibSpriteMetadata.TYPE);
		event.addAdditionalMetadata(PResourceCache.ATLAS_FILE_LOCATION, PLibMetadata.TYPE);
	}

	private static void addLayers(EntityRenderersEvent.AddLayers event)
	{
		for (var skin : event.getSkins())
		{
			var renderer = event.getPlayerRenderer(skin);
			if (renderer != null)
			{
				PLibArmorHandler.addLayers(renderer);
			}
		}
		for (var type : event.getEntityTypes())
		{
			PLibArmorHandler.addLayers(event.getRenderer(type));
		}
	}

	private static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event)
	{
		event.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
				PLibArmorHandler::extractAttachmentRenderData);
	}
}
