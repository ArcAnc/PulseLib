/**
 * @author ArcAnc
 * Created at: 09.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration.player;

import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.deformer.*;
import com.arcanc.pulselib.content.player.animation.*;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import java.util.List;


public final class PPlayerAcrobaticDemo
{
	public static final ResourceLocation ID = PLibDatabase.rl("demo/player_acrobatic");
	public static final PModelData MODEL = PModelData.direct(
			PLibDatabase.rl("player/demo/acrobatic"), PGltfModelLoader.INSTANCE.id());
	private static final PRawAnimation ANIMATION = PRawAnimation.begin().thenPlay("player_actobatic").build();
	private static final PChannelReference<Float> KNEE_BEND = new PChannelReference<>("acrobatic_knee_bend", 0.0f);
	private static final PDeformerStack BENT_LEG = PDeformerStack.compile(List.of(
			new PDeformerInstance<>(PBendDeformer.INSTANCE, new PBendDefinition(
					new Vector3f(0.0f, 6.0f, 0.0f),
					new Vector3f(0.0f, 1.0f, 0.0f),
					new Vector3f(1.0f, 0.0f, 0.0f),
					6.0f, 0.0f, KNEE_BEND))));
	public static final KeyMapping KEY = new KeyMapping(
			"key.pulselib.player_acrobatic",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			KeyMapping.CATEGORY_MISC);

	private PPlayerAcrobaticDemo()
	{
	}

	public static void registerAnimation(PPlayerAnimationRegistrar registrar)
	{
		registrar.register(ID, PPlayerAnimationDefinition.builder(MODEL).
				when(player -> player == Minecraft.getInstance().player).
				bind(PPlayerPart.ROOT, "root").
				bind(PPlayerPart.RIGHT_ARM, "right_arm").
				bind(PPlayerPart.LEFT_ARM, "left_arm").
				bind(PPlayerPart.RIGHT_LEG, "right_leg").
				bind(PPlayerPart.LEFT_LEG, "left_leg").
				bind(PPlayerPart.BODY, "body").
				mask(PPlayerPart.ROOT, PPlayerPart.RIGHT_ARM, PPlayerPart.LEFT_ARM, PPlayerPart.RIGHT_LEG, PPlayerPart.LEFT_LEG, PPlayerPart.BODY).
				deform(PPlayerPart.RIGHT_LEG, BENT_LEG, PPlayerAcrobaticDemo::deformerValue).
				deform(PPlayerPart.LEFT_LEG, BENT_LEG, PPlayerAcrobaticDemo::deformerValue).
				rootPivot(0.0f, 0.5f, 0.0f).
				blendMode(PPlayerAnimationBlendMode.OVERRIDE).
				controllers(controllers -> controllers.add("acrobatic", () -> state ->
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
		if (handle != null && !handle.isPlaying("acrobatic"))
			handle.play("acrobatic", ANIMATION);
	}

	private static float deformerValue(PPlayerAnimationDeformerContext context, PChannelReference<Float> reference)
	{
		if (!reference.name().equals(KNEE_BEND.name()))
			return reference.defaultValue();
		float seconds = context.controllerSeconds("acrobatic");
		float bendIn = smoothStep((seconds - 3.8f) / 0.5f);
		float bendOut = 1.0f - smoothStep((seconds - 4.8f) / 0.7f);
		return bendIn * bendOut * 1.25f;
	}

	private static float smoothStep(float value)
	{
		float clamped = Math.clamp(value, 0.0f, 1.0f);
		return clamped * clamped * (3.0f - 2.0f * clamped);
	}
}
