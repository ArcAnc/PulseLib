/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PTransitionTestSupport
{
	static PPose pose(float value)
	{
		PPose pose = new PPose(2, 1);
		pose.setAnimated(0, new Vector3f(value, value * 2, -value),
				new Quaternionf().rotationXYZ(value * 0.02f, -value * 0.03f, value * 0.04f),
				new Vector3f(1 + value * 0.01f, 1 + value * 0.02f, 1 + value * 0.03f));
		pose.scalarChannel(0, value);
		return pose;
	}

	static PPoseMixer.Layer layer(PPose pose)
	{
		return new PPoseMixer.Layer(pose, PPoseBlendMode.OVERRIDE, 0, 1.0f);
	}

	static void assertPose(PPose expected, PPose actual)
	{
		assertEquals(expected.boneCount(), actual.boneCount());
		for (int bone = 0; bone < expected.boneCount(); bone++)
		{
			assertTrue(expected.translation(bone).equals(actual.translation(bone), 0.0001f), "translation " + bone + ": " + actual.translation(bone));
			assertTrue(expected.scale(bone).equals(actual.scale(bone), 0.0001f), "scale " + bone);
			assertEquals(1.0f, Math.abs(expected.rotation(bone).dot(actual.rotation(bone))), 0.00002f, "rotation " + bone);
		}
		assertEquals(expected.scalarChannelCount(), actual.scalarChannelCount());
		for (int channel = 0; channel < expected.scalarChannelCount(); channel++)
		{
			assertEquals(expected.scalarChannel(channel), actual.scalarChannel(channel), 0.0001f);
		}
	}

	static PPose expectedBlend(PPose source, PPose target, float alpha)
	{
		PPose expected = new PPose(source.boneCount(), source.scalarChannelCount());
		for (int bone = 0; bone < source.boneCount(); bone++)
		{
			expected.set(bone, new Vector3f(source.translation(bone)).lerp(target.translation(bone), alpha),
					new Quaternionf(source.rotation(bone)).slerp(target.rotation(bone), alpha).normalize(),
					new Vector3f(source.scale(bone)).lerp(target.scale(bone), alpha));
		}
		for (int channel = 0; channel < source.scalarChannelCount(); channel++)
		{
			expected.scalarChannel(channel, source.scalarChannel(channel) * (1 - alpha) + target.scalarChannel(channel) * alpha);
		}
		return expected;
	}

	static PAnimation clip(String name, float start, float end, boolean trs)
	{
		PPose first = pose(start);
		PPose last = pose(end);
		Map<String, PAnimationTrack<?>> tracks = new LinkedHashMap<>();
		tracks.put("position", vectorTrack(PLibRegistration.AnimationChannelReg.POSITION, first.translation(0), last.translation(0)));
		if (trs)
		{
			tracks.put("scale", vectorTrack(PLibRegistration.AnimationChannelReg.SCALE, first.scale(0), last.scale(0)));
			PAnimationValue<Quaternionf> from = (context, destination) -> destination.set(first.rotation(0));
			PAnimationValue<Quaternionf> to = (context, destination) -> destination.set(last.rotation(0));
			tracks.put("rotation", new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.ROTATION,
					List.of(new PKeyframe<>(0, from, from, PInterpolationType.LINEAR), new PKeyframe<>(100, to, to, PInterpolationType.LINEAR))));
		}
		return new PAnimation(name, 100, Map.of("root", new PBoneAnimation(UUID.randomUUID(), tracks)));
	}

	private static PAnimationTrack<Vector3f> vectorTrack(PAnimationChannelType<Vector3f> channel, Vector3f first, Vector3f last)
	{
		PAnimationValue<Vector3f> from = (context, destination) -> destination.set(first);
		PAnimationValue<Vector3f> to = (context, destination) -> destination.set(last);
		return new PAnimationTrack<>(channel, List.of(new PKeyframe<>(0, from, from, PInterpolationType.LINEAR),
				new PKeyframe<>(100, to, to, PInterpolationType.LINEAR)));
	}

	static PBakedModel model()
	{
		return new PBakedModel(List.of(new PBakedBone("root", new Vector3f(2, 3, 4), new Quaternionf().rotationY(0.1f), List.of(), null, List.of()),
				new PBakedBone("untouched", new Vector3f(3, 2, 1), new Quaternionf(), List.of(), null, List.of())),
				Map.of("idle", clip("idle", 10, 20, true), "walk", clip("walk", 20, 40, true),
						"run", clip("run", 30, 60, true), "attack", clip("attack", 40, 80, true), "overlay", clip("overlay", 5, 15, true)));
	}

	static Object field(Object object, String name)
	{
		try
		{
			Field field = object.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return field.get(object);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	static final class Dummy implements PAnimatable<Dummy>
	{
		@Override
		public PAnimationManager<Dummy> getAnimationManager(AnimManagerKey key)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<Dummy> registrar)
		{
		}
	}
}
