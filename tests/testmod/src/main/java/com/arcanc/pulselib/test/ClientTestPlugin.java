package com.arcanc.pulselib.test;

import com.arcanc.pulselib.api.plugin.IPulseClientPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.*;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.*;
import com.arcanc.pulselib.content.registration.item.*;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.content.registration.player.*;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.attachment.*;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.attachments.*;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidBindings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.List;

@PulsePlugin
public class ClientTestPlugin implements IPulseClientPlugin
{
	static
	{
		if (!PulseServices.PLATFORM.isClient())
			throw new AssertionError("CLIENT CLASS LOADED ON SERVER");
	}

	public static final PModelData GLTF = TestBlockItemRenderer.MODEL_DATA;
	public static final PModelData GECKO = PModelData.direct(PLibDatabase.rl("player/demo/player_flip"), PGeckoModelLoader.INSTANCE.id());
	public static int animatedRenders;
	public static int animatedRegistrations;
	public static int layerRenders;
	public static int stageFrames;
	public static int constructions;
	public static int models;
	public static int attachments;
	public static int playerAnimations;

	public ClientTestPlugin()
	{
		if (++constructions != 1) throw new AssertionError("Client plugin constructed more than once");
		PLibDatabase.LOGGER.info("PULSE TEST client demo plugin constructed");
	}

	public static ResourceLocation id(String path) { return DemoContent.id(path); }
	@Override public ResourceLocation getPluginId() { return id("client"); }

	@Override public void registerModels(PModelRegistrar registrar)
	{
		models++;
		registrar.model(TestEntityRender.MODEL_DATA).texture("tube", TestEntityRender.TUBE).texture("sphere", TestEntityRender.SPHERE)
				.texture("torus", TestEntityRender.TORUS).texture("0", TestEntityRender.ZERO).texture("armor/0", TestEntityRender.ARMOR);
		registrar.model(PTestArmor.MODEL_DATA).texture("0", TestEntityRender.ARMOR);
		registrar.model(TestBlockEntityRenderer.MODEL_DATA).texture("cube_texture", TestBlockEntityRenderer.CUBE)
				.texture("torus_texture", TestBlockEntityRenderer.TORUS).texture("tube_texture", TestBlockEntityRenderer.TUBE)
				.texture("pyramid_texture", TestBlockEntityRenderer.PYRAMID);
		registrar.model(TestBlockItemRenderer.MODEL_DATA).texture("pyramid", TestBlockItemRenderer.PYRAMID).texture("circle", TestBlockItemRenderer.CIRCLE);
		registrar.model(TestArmor.MODEL_DATA).texture("0", TestArmor.TEXTURE);
		registrar.model(TestTailItem.MODEL_DATA).texture("0", TestTailItem.TEXTURE);
		registrar.model(PPlayerBallDemo.MODEL_DATA).texture("0", PPlayerBallDemo.TEXTURE);
		registrar.model(PPlayerAcrobaticDemo.MODEL).texture("player_skin_template", PLibDatabase.rl("player/demo/acrobatic/player_skin_template"));
		// Original animation-only Gecko player rig coexists with the glTF demos.
		registrar.model(GECKO).texture("0", TestEntityRender.ZERO);
		PLibDatabase.LOGGER.info("PULSE TEST demo model declarations {}", models);
	}

	@Override public void registerAttachments(PAttachmentRegistrar registrar)
	{
		if (++attachments != 1) throw new AssertionError("Attachment phase repeated");
		registrar.registerLiving(DemoContent.helmet.get(), new PLivingAttachmentDefinition(TestArmor.MODEL_DATA,
				PLivingAttachmentSources.equipmentSlot(EquipmentSlot.HEAD), List.of(PHumanoidBindings.head("head")), TestArmor::resolveArmorRender, true));
		registrar.registerLiving(DemoContent.chestplate.get(), new PLivingAttachmentDefinition(TestArmor.MODEL_DATA,
				PLivingAttachmentSources.equipmentSlot(EquipmentSlot.CHEST), List.of(PHumanoidBindings.rightArm("right_arm")), TestArmor::resolveArmorRender, false));
		registrar.registerLiving(DemoContent.leggings.get(), new PLivingAttachmentDefinition(TestArmor.MODEL_DATA,
				PLivingAttachmentSources.equipmentSlot(EquipmentSlot.LEGS), List.of(PHumanoidBindings.rightLeg("right_leg")), TestArmor::resolveArmorRender, true));
		registrar.registerLiving(DemoContent.tail.get(), TestTailItem.createDefinition());
	}

	@Override public void registerPlayerAnimations(PPlayerAnimationRegistrar registrar)
	{
		if (++playerAnimations != 1) throw new AssertionError("Player animation phase repeated");
		PPlayerBallDemo.registerAnimation(registrar);
		PPlayerAcrobaticDemo.registerAnimation(registrar);
	}

	@Override public void registerAnimatedAttachments(PAnimatedAttachmentRegistrar registrar)
	{
		if (++animatedRegistrations != 1) throw new AssertionError("Animated attachment phase repeated");
		// Observe the existing ball demo's hand socket; its ball mesh is rendered by the definition.
		registrar.register(new PPlayerAnimatedAttachmentRenderer()
		{
			@Override public com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor anchor() { return PPlayerAnimationAnchors.RIGHT_HAND; }
			@Override public void render(PPlayerAnimatedAttachmentContext context) { animatedRenders++; }
		});
	}
}
