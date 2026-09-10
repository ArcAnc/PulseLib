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
import com.arcanc.pulselib.content.player.animation.*;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class PPlayerBallDemo
{
	private static final Identifier ID = PLibDatabase.rl("demo/player_ball_toss");
	public static final Identifier TEXTURE = PLibDatabase.rl("player/demo/test_ball_model/0");
	private static final PModelData MODEL = new PModelData.Builder(
			PLibDatabase.rl("glmodels/player/demo/test_ball_model.gltf"), "", PGltfModelLoader.INSTANCE.id()).build();
	private static final PRawAnimation BALL_TOSS = PRawAnimation.begin().thenPlay("ball_toss").build();
	private static final KeyMapping KEY = new KeyMapping(
			"key.pulselib.player_ball_toss",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_V,
			KeyMapping.Category.MISC);

	private PPlayerBallDemo()
	{
	}

	public static void register(IEventBus modEventBus)
	{
		if (FMLLoader.getCurrent().isProduction())
			return;
		modEventBus.addListener(PPlayerBallDemo::registerKeyMapping);
		modEventBus.addListener(PPlayerBallDemo::registerAnimation);
		NeoForge.EVENT_BUS.addListener(PPlayerBallDemo::clientTick);
	}

	private static void registerKeyMapping(RegisterKeyMappingsEvent event)
	{
		event.register(KEY);
	}

	private static void registerAnimation(PulseLibEvents.PlayerAnimationRegistrationEvent event)
	{
		event.registration().register(ID, PPlayerAnimationDefinition.builder(MODEL).
						when(player -> player == Minecraft.getInstance().player).
						bind(PPlayerPart.HEAD, "head").
						bind(PPlayerPart.RIGHT_ARM, "right_arm").
						bind(PPlayerPart.LEFT_ARM, "left_arm").
						mask(PPlayerPart.RIGHT_ARM, PPlayerPart.LEFT_ARM).
						anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "fp_camera").
						anchor(PPlayerAnimationAnchors.RIGHT_ITEM, "right_hand").
						anchor(PPlayerAnimationAnchors.LEFT_ITEM, "left_hand").
						firstPerson(PPlayerFirstPersonSettings.ENABLED).
						hideItemInHands((player, hand, stack) ->
								hand == InteractionHand.MAIN_HAND &&
								stack.getItem() == PLibRegistration.ItemReg.TEST_ITEM.get()).
						controllers(registrar -> registrar.add("ball_toss", () -> state ->
								state.controller().isStopped() ? ControllerState.STOP : ControllerState.PLAY)).
				build());
	}

	private static void clientTick(ClientTickEvent.Post event)
	{
		Player player = Minecraft.getInstance().player;
		if (player == null || !KEY.consumeClick())
			return;
		PPlayerAnimationHandle handle = PPlayerAnimations.getHandle(player, ID);
		if (handle != null && !handle.isPlaying("ball_toss"))
			handle.play("ball_toss", BALL_TOSS);
	}
}
