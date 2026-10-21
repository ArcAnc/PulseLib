/**
 * @author ArcAnc
 * Created at: 18.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.model.PBone;
import com.arcanc.pulselib.content.model.PModel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PGeckoModelParser;
import com.arcanc.pulselib.data.gecko.PMolangVectorValue;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PCatmullRomSamplingTest
{
	@Test
	void geckoFixtureMatchesIndependentUniformSplineReference() throws IOException
	{
		PModel model = new PModel();
		UUID id = UUID.randomUUID();
		model.bones.put(id, new PBone(id, "root", new Vector3f(), new Quaternionf()));
		PAnimation animation;
		try (var stream = getClass().getResourceAsStream("/animations/catmullrom.animation.json"))
		{
			animation = PGeckoModelParser.parseAnimations(stream, model).get("curve");
		}
		// P0=-1, P1=0, P2=2, P3=0; P(t)=(3t+6t^2-5t^3)/2.
		// Timestamps 0,20,60,120 use uniform point weights and local segment alpha.
		float[] expected = {0, 0.5234375f, 1.1875f, 1.7578125f, 2};
		for (int quarter = 0; quarter <= 4; quarter++)
		{
			BoneFrame frame = sample(animation, 20 + quarter * 10);
			assertEquals(expected[quarter], frame.translation().x, 0.000001f);
			assertEquals(expected[quarter], frame.scale().x, 0.000001f);
		}
		assertEquals(-0.625f, sample(animation, 10).translation().x, 0.000001f);
		assertEquals(1.125f, sample(animation, 90).translation().x, 0.000001f);
		assertEquals(-1, sample(animation, -1).translation().x);
		assertEquals(0, sample(animation, 121).translation().x);
		assertEquals(0, sample(animation, 20 - 0.0001f).translation().x, 0.00002f);
		assertEquals(2, sample(animation, 60 - 0.0001f).translation().x, 0.00002f);
	}

	@Test
	void collinearKeysAndOvershootAreNotClamped()
	{
		PAnimation line = clip(List.of(key(0, 0), key(10, 1), key(20, 2), key(30, 3)));
		for (int quarter = 0; quarter <= 4; quarter++)
		{
			assertEquals(1 + quarter * 0.25f, sample(line, 10 + quarter * 2.5f).translation().x, 0.000001f);
		}
		PAnimation peak = clip(List.of(key(0, 0), key(10, 1), key(20, 1), key(30, 0)));
		assertEquals(1.125f, sample(peak, 15).translation().x, 0.000001f);
		PAnimation negative = clip(List.of(key(0, 0), key(10, -1), key(20, -1), key(30, 0)));
		assertEquals(-1.125f, sample(negative, 15).translation().x, 0.000001f);
	}

	@Test
	void prePostDiscontinuitiesAndDuplicateTimestampsKeepTheirSides()
	{
		PKeyframe<Vector3f> split = new PKeyframe<>(10, value(1), value(3), PInterpolationType.CATMULLROM);
		PAnimation animation = clip(List.of(key(0, 0), split, key(20, 4), key(30, 5)));
		assertEquals(1, sample(animation, 10 - 0.0001f).translation().x, 0.0001f);
		assertEquals(3, sample(animation, 10).translation().x);
		assertEquals(3, sample(animation, 10 + 0.0001f).translation().x, 0.0001f);
		PAnimation repeated = clip(List.of(key(0, 0), key(10, 1), key(10, 3), key(20, 4)));
		assertEquals(3, sample(repeated, 10).translation().x);
		assertEquals(4, sample(repeated, 20).translation().x);
	}

	@Test
	void molangControlPointsUseCurrentFrameContext()
	{
		PAnimationValue<Vector3f> dynamic = new PMolangVectorValue(MolangParser.parse("q.anim_time * 2"),
				MolangParser.parse("0"), MolangParser.parse("0"), PVectorConversion.IDENTITY);
		PAnimation animation = clip(List.of(key(0, 0), key(10, 1), new PKeyframe<>(20, dynamic, dynamic, PInterpolationType.CATMULLROM), key(30, 0)));
		var context = new MolangParser.Context().query("anim_time", 0.5f);
		assertEquals(1.125f, animation.calculateBoneTransformations("root", 15, PInterpolationType.LINEAR, context).translation().x, 0.000001f);
		context.query("anim_time", 1);
		assertEquals(1.6875f, animation.calculateBoneTransformations("root", 15, PInterpolationType.LINEAR, context).translation().x, 0.000001f);
	}

	@Test
	void twoPointLinearStepAndRegisteredCustomInterpolationKeepTheirApi()
	{
		PInterpolationType custom = PInterpolationType.register("pulse_test_squared", alpha -> alpha * alpha);
		for (PInterpolation interpolation : new PInterpolation[]{PInterpolationType.LINEAR, PInterpolationType.STEP, custom})
		{
			PAnimation animation = clip(List.of(new PKeyframe<>(0, value(0), value(0), interpolation), key(10, 4)));
			float expected = interpolation == PInterpolationType.LINEAR ? 2 : interpolation == custom ? 1 : 0;
			assertEquals(expected, sample(animation, 5).translation().x, 0.000001f);
			assertEquals(4, sample(animation, 10).translation().x);
		}
		assertEquals(0, PInterpolationType.catmullRom(0));
		assertEquals(1, PInterpolationType.catmullRom(1));
	}

	private static PAnimation clip(List<PKeyframe<Vector3f>> keys)
	{
		return new PAnimation("curve", 30, Map.of("root", new PBoneAnimation(UUID.randomUUID(),
				Map.of("position", new PAnimationTrack<>(PLibRegistration.AnimationChannelReg.POSITION, keys)))));
	}

	private static PKeyframe<Vector3f> key(float time, float scalar)
	{
		PAnimationValue<Vector3f> value = value(scalar);
		return new PKeyframe<>(time, value, value, PInterpolationType.CATMULLROM);
	}

	private static PAnimationValue<Vector3f> value(float scalar)
	{
		return (context, destination) -> destination.set(scalar, 0, 0);
	}

	private static BoneFrame sample(PAnimation animation, float time)
	{
		return animation.calculateBoneTransformations("root", time, PInterpolationType.LINEAR);
	}
}
