/**
 * @author ArcAnc
 * Created at: 05.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid.armor;


import com.arcanc.pulselib.util.attachments.PLivingAttachmentLayer;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAttachmentLayer;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentLayer;
import com.google.common.reflect.TypeToken;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.List;

public class PLibArmorHandler
{
	public static void register(IEventBus modEventBus)
	{
		modEventBus.addListener(PLibArmorHandler :: addArmorLayers);
		modEventBus.addListener(PLibArmorHandler :: registerRenderStateModifiers);
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void addArmorLayers(final EntityRenderersEvent.AddLayers event)
	{
		for (PlayerModelType skin : event.getSkins())
		{
			AvatarRenderer<?> renderer = event.getPlayerRenderer(skin);
			if (renderer != null)
			{
				renderer.addLayer(new PHumanoidAttachmentLayer(renderer));
				renderer.addLayer(new PPlayerAnimatedAttachmentLayer(renderer));
			}
		}
		
		for (var entityType : event.getEntityTypes())
		{
			EntityRenderer<?, ?> renderer = event.getRenderer(entityType);
			if (renderer instanceof LivingEntityRenderer livingRenderer &&
					livingRenderer.getModel() instanceof HumanoidModel<?>)
				livingRenderer.addLayer(new PHumanoidAttachmentLayer(livingRenderer));
			else if (renderer instanceof LivingEntityRenderer livingRenderer &&
					livingRenderer.getModel() instanceof EntityModel<?>)
				livingRenderer.addLayer(new PLivingAttachmentLayer(livingRenderer));
		}
	}
	
	private static void registerRenderStateModifiers(final RegisterRenderStateModifiersEvent event)
	{
		event.registerEntityModifier(
				new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
				PLibArmorHandler :: extractAttachmentRenderData);
	}
	
	private static void extractAttachmentRenderData(LivingEntity entity, LivingEntityRenderState state)
	{
		List<PLivingAttachmentLayer.RenderEntry> entries = PLivingAttachmentLayer.extractRenderEntries(entity, state.partialTick);
		state.setRenderData(PLivingAttachmentLayer.RENDER_DATA, entries.isEmpty() ? null : entries);
	}
	
}
