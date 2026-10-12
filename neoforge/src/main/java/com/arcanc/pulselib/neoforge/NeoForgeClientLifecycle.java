package com.arcanc.pulselib.neoforge;

import com.arcanc.pulselib.content.animatable.PLibAnimationTicker;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PLibArmorHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMaterialAtlasesEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.io.IOException;

public final class NeoForgeClientLifecycle
{
	public static void register(IEventBus bus)
	{
		bus.addListener(NeoForgeClientLifecycle::shaders);
		bus.addListener(NeoForgeClientLifecycle::atlases);
		bus.addListener(NeoForgeClientLifecycle::layers);
		NeoForge.EVENT_BUS.addListener(NeoForgeClientLifecycle::tick);
		NeoForge.EVENT_BUS.addListener(NeoForgeClientLifecycle::afterTick);
		NeoForge.EVENT_BUS.addListener(NeoForgeClientLifecycle::entityRemoved);
		NeoForge.EVENT_BUS.addListener(NeoForgeClientLifecycle::render);
	}

	private static void shaders(RegisterShadersEvent event)
	{
		try
		{
			PRenderTypes.ShadersProvider.registerShaders((id, format, setter) ->
					event.registerShader(new ShaderInstance(event.getResourceProvider(), id, format), setter));
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot register PulseLib shaders", exception);
		}
	}

	private static void atlases(RegisterMaterialAtlasesEvent event)
	{
		event.register(PResourceCache.ATLAS_LOCATION, PResourceCache.ATLAS_FILE_LOCATION);
	}

	private static void layers(EntityRenderersEvent.AddLayers event)
	{
		event.getSkins().forEach(skin -> {
			var renderer = event.getSkin(skin);
			if (renderer != null)
				PLibArmorHandler.addLayers(renderer);
		});
		event.getEntityTypes().forEach(type -> PLibArmorHandler.addLayers(event.getRenderer(type)));
	}

	private static void tick(LevelTickEvent.Pre event)
	{
		if (event.getLevel() instanceof ClientLevel level)
			PLibAnimationTicker.clientTick(level);
	}

	private static void afterTick(ClientTickEvent.Post event)
	{
		PLibAnimationTicker.afterClientTick();
	}

	private static void entityRemoved(EntityLeaveLevelEvent event)
	{
		if (event.getLevel().isClientSide())
			InstanceAnimationManager.removeManager(event.getEntity());
	}

	private static void render(RenderLevelStageEvent event)
	{
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)
			PRenderStagesHandler.afterBlockEntities();
		else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			PRenderStagesHandler.afterParticles();
		else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER)
		{
			var destination = event.getLevelRenderer().getWeatherTarget();
			PRenderStagesHandler.afterWeather(destination == null ? Minecraft.getInstance().getMainRenderTarget() : destination);
		}
	}
}
