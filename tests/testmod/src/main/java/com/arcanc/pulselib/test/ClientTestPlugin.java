package com.arcanc.pulselib.test;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.attachment.*;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.attachments.*;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidBindings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import java.util.List;

@PulsePlugin
public class ClientTestPlugin implements IPulseClientPlugin
{
	static
	{
		if (!PulseServices.PLATFORM.isClient())
			throw new AssertionError("CLIENT CLASS LOADED ON SERVER");
	}

	public static final PModelData GLTF = PModelData.direct(id("triangle"));
	public static final PModelData GECKO = PModelData.direct(id("cube"), PGeckoModelLoader.INSTANCE.id());
	public static final PPlayerAnimationAnchor ANCHOR = new PPlayerAnimationAnchor(id("socket"));
	public static int animatedRenders;
	public static int animatedRegistrations;
	public static int layerRenders;
	public static int constructions;
	public static int models;
	public static int attachments;
	public static int playerAnimations;

	public ClientTestPlugin()
	{
		if (++constructions != 1)
			throw new AssertionError("Client plugin constructed more than once");
		PLibDatabase.LOGGER.info("PULSE TEST client plugin constructed");
	}

	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath("pulselibtest", path);
	}

	@Override public ResourceLocation getPluginId() { return id("client"); }

	@Override
	public void registerModels(PModelRegistrar registrar)
	{
		models++;
		registrar.model(GLTF).texture("base", id("test"));
		registrar.model(GECKO).texture("0", id("test"));
		PLibDatabase.LOGGER.info("PULSE TEST model declarations {}", models);
	}

	@Override
	public void registerAttachments(PAttachmentRegistrar registrar)
	{
		if (++attachments != 1)
			throw new AssertionError("Attachment phase repeated");
		registrar.registerLiving(Items.DIAMOND_HELMET, new PLivingAttachmentDefinition(GECKO,
				PLivingAttachmentSources.equipmentSlot(EquipmentSlot.HEAD), List.of(PHumanoidBindings.head("root")),
				PLivingMeshRenderResolvers.defaultLit(), true));
	}

	@Override
	public void registerPlayerAnimations(PPlayerAnimationRegistrar registrar)
	{
		if (++playerAnimations != 1)
			throw new AssertionError("Player animation phase repeated");
		registrar.register(id("player"), PPlayerAnimationDefinition.builder(GECKO).bind(PPlayerPart.BODY, "root").anchor(ANCHOR, "root").controllers(controllers ->
				controllers.add("idle", () -> state -> ControllerState.PLAY)).build());
	}

	@Override
	public void registerAnimatedAttachments(PAnimatedAttachmentRegistrar registrar)
	{
		if (++animatedRegistrations != 1)
			throw new AssertionError("Animated attachment phase repeated");
		registrar.register(new PPlayerAnimatedAttachmentRenderer()
		{
			@Override public PPlayerAnimationAnchor anchor() { return ANCHOR; }
			@Override public void render(PPlayerAnimatedAttachmentContext context) { animatedRenders++; }
		});
	}
}
