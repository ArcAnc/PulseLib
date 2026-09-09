/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.player;


import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.event.PulseLibEvents;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationHandle;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class PPlayerAcrobaticDemo
{
	private static final Identifier ID = PLibDatabase.rl("demo/player_acrobatic");
	private static final PModelData MODEL = new PModelData.Builder(
			PLibDatabase.rl("glmodels/player/demo/acrobatic.gltf"), "", PGltfModelLoader.INSTANCE.id()).build();
	private static final PRawAnimation ACROBATIC = PRawAnimation.begin().thenPlay("player_actobatic").build();
	private static final KeyMapping KEY = new KeyMapping(
			"key.pulselib.player_acrobatic",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			KeyMapping.Category.MISC);

	private PPlayerAcrobaticDemo()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		if (FMLLoader.getCurrent().isProduction())
			return;
		modEventBus.addListener(PPlayerAcrobaticDemo::registerKeyMapping);
		modEventBus.addListener(PPlayerAcrobaticDemo::registerAnimation);
		NeoForge.EVENT_BUS.addListener(PPlayerAcrobaticDemo::clientTick);
	}

	private static void registerKeyMapping(RegisterKeyMappingsEvent event)
	{
		event.register(KEY);
	}

	private static void registerAnimation(PulseLibEvents.PlayerAnimationRegistrationEvent event)
	{
		event.registration().register(ID, PPlayerAnimationDefinition.builder(MODEL).
				when(player -> player == Minecraft.getInstance().player).
				bind(PPlayerPart.ROOT, "root").
				bind(PPlayerPart.HEAD, "head").
				bind(PPlayerPart.BODY, "body").
				bind(PPlayerPart.RIGHT_ARM, "right_arm").
				bind(PPlayerPart.LEFT_ARM, "left_arm").
				bind(PPlayerPart.RIGHT_LEG, "right_leg").
				bind(PPlayerPart.LEFT_LEG, "left_leg").
				anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "head").
				controllers(registrar -> registrar.add("acrobatic", () -> state ->
						state.controller().isStopped() ? ControllerState.STOP : ControllerState.PLAY)).
				build());
	}

	private static void clientTick(ClientTickEvent.Post event)
	{
		Player player = Minecraft.getInstance().player;
		if (player == null || !KEY.consumeClick())
			return;
		PPlayerAnimationHandle handle = PPlayerAnimations.getHandle(player, ID);
		if (handle != null && !handle.isPlaying("acrobatic"))
			handle.play("acrobatic", ACROBATIC);
	}
}
