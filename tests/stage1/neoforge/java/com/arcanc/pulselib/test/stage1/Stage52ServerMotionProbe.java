/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationTrack;
import com.arcanc.pulselib.content.model.animation.PAnimationValue;
import com.arcanc.pulselib.content.model.animation.PBoneAnimation;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.model.animation.PKeyframe;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationMask;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.util.PLibDatabase;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Dedicated-server extraction witnesses; no client model cache or rendering is invoked. */
public final class Stage52ServerMotionProbe
{
	private Stage52ServerMotionProbe()
	{
	}

	public static void verify(MinecraftServer server)
	{
		PAnimationValue<Vector3f> from = (context, destination) -> destination.zero();
		PAnimationValue<Vector3f> to = (context, destination) -> destination.set(0, 0, 4);
		var track = new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION, List.of(
				new PKeyframe<>(0, from, from, PInterpolationType.LINEAR),
				new PKeyframe<>(20, to, to, PInterpolationType.LINEAR)));
		var clip = new PAnimation("server_motion", 20, Map.of("root",
				new PBoneAnimation(UUID.randomUUID(), Map.of("position", track))));
		var baked = new PBakedModel(List.of(new PBakedBone("root", new Vector3f(),
				new Quaternionf(), List.of(), null, List.of())), Map.of(clip.name(), clip));
		var model = new PModelData(PLibDatabase.rl("stage525a/server_motion"), PModelData.DEFAULT_MODEL_LOADER)
		{
			@Override
			public PBakedModel getModel()
			{
				return baked;
			}
		};
		var ids = new net.minecraft.resources.Identifier[] {
				PLibDatabase.rl("stage525a/root"), PLibDatabase.rl("stage525a/body") };
		for (int index = 0; index < ids.length; index++)
			PPlayerAnimations.register(ids[index], PPlayerAnimationDefinition.builder(model).
					bind(PPlayerPart.ROOT, "root").
					mask(index == 0 ? PPlayerPart.ROOT : PPlayerPart.BODY).
					itemVisibility("server_motion", PPlayerAnimationDefinition.ItemVisibilityPolicy.VISIBLE).
					build());
		int cases = 0;
		var instances = new HashSet<Object>();
		for (int owner = 0; owner < 2; owner++)
		{
			var player = FakePlayerFactory.get(server.overworld(), new GameProfile(
					UUID.fromString("525a0000-0000-4000-8000-00000000000" + owner), "MotionOwner" + owner));
			player.setPos(10 + owner, 80, -3);
			player.setDeltaMovement(0.125, -0.25, 0.5);
			player.setYRot(37 + owner);
			player.setXRot(-19);
			check(PPlayerAnimationMask.of(PPlayerPart.ROOT).contains(player, PPlayerPart.ROOT, 0) &&
					!PPlayerAnimationMask.of(PPlayerPart.BODY).contains(player, PPlayerPart.ROOT, 0), "varargs masks on server");
			var position = player.position();
			var velocity = player.getDeltaMovement();
			var box = player.getBoundingBox();
			float yaw = player.getYRot();
			float pitch = player.getXRot();
			for (int definition = 0; definition < ids.length; definition++)
			{
				var instance = PPlayerAnimations.getInstance(player, ids[definition]);
				check(instance != null && instances.add(instance) && instance.player() == player, "independent owner/definition instance");
				check(PPlayerAnimations.getInstance(player, ids[definition]) == instance, "stable manager ownership");
				check(instance.definition().appliesTo(player, PPlayerPart.ROOT, 0) == (definition == 0), "ROOT mask selection");
				for (float[] times : new float[][] {{0, 10}, {10, 20}, {20, 0}, {10, 10}})
				{
					var animation = instance.definition().modelData().getModel().animations().get("server_motion");
					var delta = animation.rootMotion("root", PInterpolationType.LINEAR).extractRootMotion(times[0], times[1]);
					check(delta.translation().distance(new Vector3f(0, 0, (times[1] - times[0]) / 5)) < 0.00001f, "nonzero/reverse/zero extraction oracle");
					check(Math.abs(delta.rotation().w - 1) < 0.00001f, "identity rotation");
					check(player.position().equals(position) && player.getDeltaMovement().equals(velocity) &&
							player.getBoundingBox().equals(box) && player.getYRot() == yaw && player.getXRot() == pitch,
							"extraction preserves position/velocity/bounds/orientation");
					cases++;
				}
			}
		}
		check(cases == 16, "complete extraction matrix");
		PLibDatabase.LOGGER.info("STAGE525A SERVER MOTION PASS: {} cases, 2 server owners, 2 definitions, ROOT masks, unchanged physics", cases);
		server.halt(false);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE525A: " + message);
	}
}
