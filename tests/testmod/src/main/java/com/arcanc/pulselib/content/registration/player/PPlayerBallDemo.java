/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.player;


import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.player.animation.*;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.test.AnimationSamplingHarness;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.helpers.PLibRenderHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class PPlayerBallDemo
{
	public static final ResourceLocation ID = PLibDatabase.rl("demo/player_ball_toss");
	public static final ResourceLocation TEXTURE = PLibDatabase.rl("player/demo/test_ball_model/0");
	public static final PModelData MODEL_DATA = PModelData.direct(
			PLibDatabase.rl("player/demo/test_ball_model"));
	private static final PRawAnimation BALL_TOSS = PRawAnimation.begin().thenPlay("ball_toss").build();
	public static final KeyMapping KEY = new KeyMapping(
			"key.pulselib.player_ball_toss",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_V,
			KeyMapping.CATEGORY_MISC);

	private PPlayerBallDemo()
	{
	}

	public static void registerAnimation(PPlayerAnimationRegistrar registrar)
	{
		registrar.register(ID, PPlayerAnimationDefinition.builder(MODEL_DATA).
						when(player -> player == PLibRenderHelper.mc().player).
						bind(PPlayerPart.HEAD, "head").
						bind(PPlayerPart.RIGHT_ARM, "right_arm").
						bind(PPlayerPart.LEFT_ARM, "left_arm").
						meshAttachment("ball").
						mask(PPlayerPart.RIGHT_ARM, PPlayerPart.LEFT_ARM).
						anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "fp_camera").
						anchor(PPlayerAnimationAnchors.RIGHT_HAND, "right_hand").
						anchor(PPlayerAnimationAnchors.LEFT_HAND, "left_hand").
						anchor(PPlayerAnimationAnchors.RIGHT_ITEM, "right_hand").
						anchor(PPlayerAnimationAnchors.LEFT_ITEM, "left_hand").
						firstPerson(PPlayerFirstPersonSettings.ENABLED).
						populateMolangContext((player, instance, controller, context, partialTick) ->
								AnimationSamplingHarness.checkTime(controller, context, partialTick)).
						itemRenderPolicy(PPlayerAnimationDefinition.ItemRenderPolicy.RENDER).
						controllers(controllers -> controllers.add("ball_toss", () -> state ->
								state.controller().isStopped() ? ControllerState.STOP : ControllerState.PLAY)).
				build());
	}

	public static void clientTick()
	{
		Player player = Minecraft.getInstance().player;
		if (player == null || !KEY.consumeClick())
			return;
		play(player);
	}

	public static void play(Player player)
	{
		PPlayerAnimationHandle handle = PPlayerAnimations.getHandle(player, ID);
		if (handle != null && !handle.isPlaying("ball_toss"))
			handle.play("ball_toss", BALL_TOSS);
	}
}
