/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.event;


import com.arcanc.pulselib.content.animatable.PLibAnimationTicker;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.resource.PModelResource;
import com.arcanc.pulselib.content.model.textures.atlas.RuntimeLoader;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.item.TestArmorItem;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.content.registration.player.PPlayerAcrobaticDemo;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.PRenderStagesHandler;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.arcanc.pulselib.util.attachments.PAttachmentAnchorResolvers;
import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorClientExtensions;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PLibArmorHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
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
		
		modEventBus.addListener(ClientEvents :: registerRenderers);
		modEventBus.addListener(ClientEvents :: registerCustomTextures);
		modEventBus.addListener(ClientEvents :: registerSpecialModels);
		
		PAttachmentAnchorResolvers.init(modEventBus);
		modEventBus.addListener(EventPriority.HIGHEST, ClientEvents :: registerSpriteSources);
		modEventBus.addListener(ClientEvents :: registerReloadListeners);
		modEventBus.addListener(ClientEvents :: registerPulseClientContent);
		modEventBus.addListener(ClientEvents :: registerClientExtensions);
		NeoForge.EVENT_BUS.addListener(ClientEvents :: playerDisconnected);
		PRenderTypes.register(modEventBus);
		PLibAnimationTicker.register(modEventBus);
		PRenderStagesHandler.register(modEventBus);
		PResourceCache.register(modEventBus);
		PLibArmorHandler.register(modEventBus);
		
		PPlayerBallDemo.register(modEventBus);
		PPlayerAcrobaticDemo.register(modEventBus);
	}
	
	/**
	 * Registers the reload listeners.
	 * @param event the event to use.
	 */
	private static void registerReloadListeners(final AddClientReloadListenersEvent event)
	{
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
		
		/*registrationEvent.registration().registerLiving(PLibRegistration.ItemReg.TEST_HAT.get(),
				new PLivingAttachmentDefinition(
				TestArmorItem.MODEL_DATA,
				PLivingAttachmentSources.equipmentSlot(EquipmentSlot.HEAD),
				List.of(PHumanoidBindings.head("head")),
				TestArmorItem :: resolveArmorRender,
				true));
		
		registrationEvent.registration().registerLiving(PLibRegistration.ItemReg.TEST_CHESTPLATE.get(),
				new PLivingAttachmentDefinition(
						TestArmorItem.MODEL_DATA,
						PLivingAttachmentSources.equipmentSlot(EquipmentSlot.CHEST),
						List.of(PHumanoidBindings.rightArm("right_arm")),
						TestArmorItem :: resolveArmorRender,
						true));
		
		registrationEvent.registration().registerLiving(PLibRegistration.ItemReg.TEST_LEGGINGS.get(),
				new PLivingAttachmentDefinition(
						TestArmorItem.MODEL_DATA,
						PLivingAttachmentSources.equipmentSlot(EquipmentSlot.LEGS),
						List.of(PHumanoidBindings.rightLeg("right_leg")),
						TestArmorItem :: resolveArmorRender,
						true));*/
		
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
		PRenderQueue.cleanUp();
		SingletonAnimationManager.cleanUp();
		InstanceAnimationManager.cleanUp();
		PPlayerAnimations.cleanUp();
	}
	
	/**
	 * Registers the sprite sources.
	 * @param event the event to use.
	 */
	private static void registerSpriteSources(final RegisterSpriteSourcesEvent event)
	{
		event.register(PLibDatabase.rl("runtime_loader"), RuntimeLoader.CODEC);
	}
	
	/**
	 * Registers the special models.
	 * @param event the event to use.
	 */
	private static void registerSpecialModels(final RegisterSpecialModelRendererEvent event)
	{
		event.register(PLibDatabase.rl("test_block"), TestBlockItemRenderer.Unbaked.MAP_CODEC);
	}
	
	/**
	 * Registers the renderers.
	 * @param event the event to use.
	 */
	private static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerBlockEntityRenderer(PLibRegistration.BETypeReg.TEST_BLOCK_ENTITY.get(), TestBlockEntityRenderer :: new);
		event.registerEntityRenderer(PLibRegistration.EntityTypeReg.TEST_ENTITY.get(), TestEntityRender :: new);
	}
	
	/**
	 * Registers the custom textures.
	 * @param event the event to use.
	 */
	private static void registerCustomTextures(final PulseLibEvents.RegisterResourceEvent event)
	{
		event.register(PModelResource.builder(PLibDatabase.rl("entity/test_entity")).
						texture("tube", TestEntityRender.TUBE).
						texture("sphere", TestEntityRender.SPHERE).
						texture("torus", TestEntityRender.TORUS).
						texture("0", TestEntityRender.ZERO).
						texture("armor/0", TestEntityRender.ARMOR));
		event.register(PModelResource.builder(PLibDatabase.rl("entity/test_entity/armor")).
						texture("0", TestEntityRender.ARMOR));
		event.register(PModelResource.builder(PLibDatabase.rl("block/test_block")).
						texture("cube_texture", TestBlockEntityRenderer.CUBE).
						texture("torus_texture", TestBlockEntityRenderer.TORUS).
						texture("tube_texture", TestBlockEntityRenderer.TUBE).
						texture("pyramid_texture", TestBlockEntityRenderer.PYRAMID));
		event.register(PModelResource.builder(PLibDatabase.rl("item/test_block")).
						texture("pyramid", TestBlockItemRenderer.PYRAMID).
						texture("circle", TestBlockItemRenderer.CIRCLE));
		event.register(PModelResource.builder(PLibDatabase.rl("entity/armor/test_armor")).
						texture("0", TestArmorItem.TEXTURE));
		event.register(PModelResource.builder(PLibDatabase.rl("player/demo/test_ball_model")).
						texture("0", PPlayerBallDemo.TEXTURE));
	}
}
