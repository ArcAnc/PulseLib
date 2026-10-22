/**
 * @author ArcAnc
 * Created at: 22.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PRootMotionSpacesTest
{
	@Test
	void extractionUsesThePreviousRootRotationAndReversesInThatSpace()
	{
		PAnimationValue<Quaternionf> rotation = (context, destination) -> destination.rotationY((float)Math.PI / 2);
		var rotationTrack = new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.ROTATION,
				List.of(new PKeyframe<>(0, rotation, rotation, PInterpolationType.LINEAR)));
		var animation = new PAnimation("move", 20, Map.of("root", new PBoneAnimation(UUID.randomUUID(),
				Map.of("position", track(new Vector3f(), new Vector3f(0, 0, 4)), "rotation", rotationTrack))));
		PRootMotionRuntime runtime = new PRootMotionRuntime(animation, "root", PInterpolationType.LINEAR);
		Vector3f forward = runtime.extractRootMotion(0, 20).translation();
		Vector3f reverse = runtime.extractRootMotion(20, 0).translation();
		assertEquals(-4, forward.x, 0.00001f);
		assertEquals(0, forward.z, 0.00001f);
		assertEquals(4, reverse.x, 0.00001f);
		assertEquals(1, Math.abs(runtime.extractRootMotion(0, 20).rotation().w), 0.00001f);
	}

	@Test
	void hierarchyDeltaDistinguishesModelSpaceFromAnExplicitRootReference()
	{
		List<PBakedBone> children = new ArrayList<>();
		PBakedBone root = new PBakedBone("root", new Vector3f(10, 0, 0),
				new Quaternionf().rotationY((float)Math.PI / 2), children, null, List.of());
		PBakedBone child = new PBakedBone("child", new Vector3f(0, 0, 2), new Quaternionf(), List.of(), root, List.of());
		children.add(child);
		var animation = new PAnimation("move", 20, Map.of("child", new PBoneAnimation(UUID.randomUUID(),
				Map.of("position", track(new Vector3f(0, 0, 3), new Vector3f(0, 0, 3))))));
		var model = new PBakedModel(List.of(root), Map.of("move", animation));
		PAnimationController<PTransitionTestSupport.Dummy> controller = new PAnimationController<>(state -> ControllerState.PLAY);
		controller.play(PRawAnimation.begin().thenLoop("move").build());
		controller.tick(new PTransitionTestSupport.Dummy(), 1, model);
		var resolver = new PAnimationPoseResolver<>(model, List.of(controller), PAnimationPoseResolver.defaultContexts(), 1);
		Vector3f modelDelta = resolver.animationDelta("child", null).translation();
		Vector3f rootDelta = resolver.animationDelta("child", "root").translation();
		assertEquals(3, modelDelta.x, 0.00001f);
		assertEquals(0, modelDelta.z, 0.00001f);
		assertEquals(0, rootDelta.x, 0.00001f);
		assertEquals(3, rootDelta.z, 0.00001f);
	}

	private static PAnimationTrack<Vector3f> track(Vector3f from, Vector3f to)
	{
		PAnimationValue<Vector3f> first = (context, destination) -> destination.set(from);
		PAnimationValue<Vector3f> last = (context, destination) -> destination.set(to);
		return new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION, List.of(
				new PKeyframe<>(0, first, first, PInterpolationType.LINEAR), new PKeyframe<>(20, last, last, PInterpolationType.LINEAR)));
	}
}
