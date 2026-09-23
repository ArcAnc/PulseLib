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
import com.arcanc.pulselib.content.animatable.PLibAnimationTicker;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.baked.PDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.baked.PGpuDeformedMeshBuffers;
import com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers;
import com.arcanc.pulselib.content.model.textures.atlas.RuntimeLoader;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
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
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

public class ClientEvents
{
	//private static final PAttachmentAnchor TEST_COW_BODY = PAttachmentAnchor.of(PLibDatabase.rl("cow_body"));
	private static boolean pulseClientContentRegistered;

	public static void registerClientEvents(final IEventBus modEventBus)
	{
		ModLoader.postEvent(new PulseLibEvents.TypeRegistrationEvent());

		//modEventBus.addListener(ClientEvents :: registerRenderers);
		//modEventBus.addListener(ClientEvents :: registerResources);

		PAttachmentAnchorResolvers.init(modEventBus);
		//registerTestCowTail();

		modEventBus.addListener(EventPriority.HIGHEST, ClientEvents :: registerSpriteSources);
		modEventBus.addListener(ClientEvents :: registerReloadListeners);
		modEventBus.addListener(ClientEvents :: registerPulseClientContent);
		modEventBus.addListener(ClientEvents :: registerClientExtensions);
		NeoForge.EVENT_BUS.addListener(ClientEvents :: playerDisconnected);
		PLibArmorHandler.register(modEventBus);
		PRenderTypes.register(modEventBus);
		PLibAnimationTicker.register(modEventBus);
		PRenderStagesHandler.register(modEventBus);
		PResourceCache.register(modEventBus);

		//PPlayerAcrobaticDemo.register(modEventBus);
		//PPlayerBallDemo.register(modEventBus);
	}

	/*private static void registerTestCowTail()
	{
		PulseAttachmentAnchorResolvers.register(CowModel.class, TEST_COW_BODY,
				(entity, model) -> entity.getType() == EntityType.COW ? ((CowModel<?>)model).body : null);
		PulseLivingAttachments.registerGlobal(TestTailItem.createDefinition(
				PLivingAttachmentSources.entityPredicate(entity -> entity.getType() == EntityType.COW),
				TEST_COW_BODY,
				new Vector3f(0, -0.4f, 0.8f),
				new Vector3f(90, 0, 0)));
	}*/

	private static void registerPulseClientContent(final FMLClientSetupEvent event)
	{
		ensurePulseClientContentRegistered();
	}

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

		/*registrationEvent.registration().livingAttachment(Registration.ItemReg.TEST_HAT.get(),
				new PLivingAttachmentDefinition(
				TestArmor.MODEL_DATA,
				PLivingAttachmentSources.equipmentSlot(EquipmentSlot.HEAD),
				List.of(PHumanoidBindings.head("head")),
				TestArmor :: resolveArmorRender,
				true));

		registrationEvent.registration().livingAttachment(Registration.ItemReg.TEST_CHESTPLATE.get(),
				new PLivingAttachmentDefinition(
						TestArmor.MODEL_DATA,
						PLivingAttachmentSources.equipmentSlot(EquipmentSlot.CHEST),
						List.of(PHumanoidBindings.rightArm("right_arm")),
						TestArmor :: resolveArmorRender,
						true));

		registrationEvent.registration().livingAttachment(Registration.ItemReg.TEST_LEGGINGS.get(),
				new PLivingAttachmentDefinition(
						TestArmor.MODEL_DATA,
						PLivingAttachmentSources.equipmentSlot(EquipmentSlot.LEGS),
						List.of(PHumanoidBindings.rightLeg("right_leg")),
						TestArmor :: resolveArmorRender,
						true));*/

		registrationEvent.registration().apply();
		playerAnimationRegistrationEvent.registration().apply();
		animatedAttachmentRegistrationEvent.registration().apply();
		pulseClientContentRegistered = true;
	}

	private static void registerClientExtensions(final RegisterClientExtensionsEvent event)
	{
		ensurePulseClientContentRegistered();

		BuiltInRegistries.ITEM.stream().
				filter(item -> item instanceof PItemAnimatable<?> || PLivingAttachments.contains(item)).
				forEach(item -> registerClientExtension(event, item));
	}

	private static void registerClientExtension(final RegisterClientExtensionsEvent event, final Item item)
	{
		if (event.isItemRegistered(item))
			return;

		IClientItemExtensions base = item instanceof PItemAnimatable<?> animatable ?
				animatable.registerClientExtension() :
				IClientItemExtensions.DEFAULT;
		IClientItemExtensions extension = PArmorClientExtensions.buildFor(item, base);

		if (extension != IClientItemExtensions.DEFAULT)
			event.registerItem(extension, item);
	}

	private static void registerReloadListeners(final RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener(PModelCache :: reload);
	}

	private static void playerDisconnected(final LevelEvent.Unload event)
	{
		if (!event.getLevel().isClientSide())
			return;
		PRenderQueue.cleanup();
		PDeformedMeshBuffers.cleanup();
		PGpuDeformedMeshBuffers.cleanup();
		PGpuDeformerBuffers.cleanup();
		InstanceAnimationManager.cleanUp();
		SingletonAnimationManager.cleanUp();
		PPlayerAnimations.cleanUp();
	}

	private static void registerSpriteSources(final RegisterSpriteSourceTypesEvent event)
	{
		event.register(PLibDatabase.rl("runtime_loader"), RuntimeLoader.TYPE);
	}

	/*private static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event)
	{
		event.registerEntityRenderer(PLibRegistration.EntityTypeReg.TEST_ENTITY.get(), TestEntityRender :: new);
		event.registerBlockEntityRenderer(PLibRegistration.BETypeReg.TEST_BLOCK_ENTITY.get(), TestBlockEntityRenderer :: new);
	}

	private static void registerResources(final PulseLibEvents.RegisterResourceEvent event)
	{
		event.model(PLibDatabase.rl("entity/test_entity")).
				texture("tube", TestEntityRender.TUBE).
				texture("sphere", TestEntityRender.SPHERE).
				texture("torus", TestEntityRender.TORUS).
				texture("0", TestEntityRender.ZERO).
				texture("armor/0", TestEntityRender.ARMOR);
		event.model(PLibDatabase.rl("entity/test_entity/armor")).
				texture("0", TestEntityRender.ARMOR);
		event.model(PLibDatabase.rl("block/test_block")).
				texture("cube_texture", TestBlockEntityRenderer.CUBE).
				texture("torus_texture", TestBlockEntityRenderer.TORUS).
				texture("tube_texture", TestBlockEntityRenderer.TUBE).
				texture("pyramid_texture", TestBlockEntityRenderer.PYRAMID);
		event.model(PLibDatabase.rl("item/test_block")).
				texture("pyramid", TestBlockItemRenderer.PYRAMID).
				texture("circle", TestBlockItemRenderer.CIRCLE);
		event.model(PLibDatabase.rl("entity/armor/test_armor")).
				texture("0", TestArmor.TEXTURE);
		event.model(PLibDatabase.rl("player/demo/test_ball_model")).
				texture("0", PPlayerBallDemo.TEXTURE);
	}*/
}
