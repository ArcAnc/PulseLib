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
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PGeckoModelParser;
import com.google.gson.JsonParser;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PGeckoEulerSamplingTest
{
	@Test
	void fullTurnsKeepTheirAuthoredDirectionAndIntermediateOrientations()
	{
		for (float[] range : new float[][]{{0, 360}, {0, 720}, {0, -360}, {90, 450}})
		{
			PAnimation animation = rotation("{\"0\": [0, " + range[0] + ", 0], \"1\": [0, " + range[1] + ", 0]}");
			for (int eighth = 0; eighth <= 8; eighth++)
			{
				float alpha = eighth / 8f;
				assertRotation(animation, alpha * 20, new MolangParser.Context(), 0, range[0] + (range[1] - range[0]) * alpha, 0);
			}
		}
	}

	@Test
	void simultaneousXyzMolangValuesAreInterpolatedBeforeQuaternionConversion()
	{
		PAnimation animation = rotation("""
				{"0": [0, 90, -90], "1": ["q.anim_time * 720", "v.turn", -450]}
				""");
		var context = new MolangParser.Context().query("anim_time", 1).variable("turn", 450);
		assertRotation(animation, 5, context, 180, 180, -180);
		context.query("anim_time", 0.5f).variable("turn", -270);
		assertRotation(animation, 5, context, 90, 0, -180);
	}

	@Test
	void preAndPostRetainIndependentTurns()
	{
		PAnimation animation = rotation("""
				{"0": [0, 0, 0], "1": {"pre": [0, 360, 0], "post": [0, 720, 0]}, "2": [0, 1080, 0]}
				""");
		for (int quarter = 0; quarter <= 4; quarter++)
		{
			assertRotation(animation, quarter * 5, new MolangParser.Context(), 0, quarter == 4 ? 720 : quarter * 90, 0);
			assertRotation(animation, 20 + quarter * 5, new MolangParser.Context(), 0, 720 + quarter * 90, 0);
		}
	}

	@Test
	void catmullRomStepAndCustomEasingOperateOnEulerValues()
	{
		PAnimation spline = rotation("""
				{"0": {"vector": [0, -360, 0], "lerp_mode": "catmullrom"},
				 "1": {"vector": [0, 0, 0], "lerp_mode": "catmullrom"},
				 "2": {"vector": [0, 360, 0], "lerp_mode": "catmullrom"},
				 "3": {"vector": [0, 720, 0], "lerp_mode": "catmullrom"}}
				""");
		for (int quarter = 0; quarter <= 4; quarter++)
		{
			assertRotation(spline, 20 + quarter * 5, new MolangParser.Context(), 0, quarter * 90, 0);
		}
		PAnimation step = rotation("{\"0\": {\"vector\": [0, 0, 0], \"lerp_mode\": \"step\"}, \"1\": [0, 360, 0]}");
		assertRotation(step, 15, new MolangParser.Context(), 0, 0, 0);
		PInterpolationType.register("pulse_test_euler_square", alpha -> alpha * alpha);
		PAnimation eased = rotation("{\"0\": {\"vector\": [0, 0, 0], \"lerp_mode\": \"pulse_test_euler_square\"}, \"1\": [0, 720, 0]}");
		assertRotation(eased, 10, new MolangParser.Context(), 0, 180, 0);
	}

	private static PAnimation rotation(String channel)
	{
		PModel model = new PModel();
		UUID id = UUID.randomUUID();
		model.bones.put(id, new PBone(id, "root", new Vector3f(), new Quaternionf()));
		return PGeckoModelParser.parseAnimations(JsonParser.parseString(
				"{\"animations\": {\"turn\": {\"bones\": {\"root\": {\"rotation\": " + channel + "}}}}}"), model).get("turn");
	}

	private static void assertRotation(PAnimation animation, float time, MolangParser.Context context, float x, float y, float z)
	{
		Quaternionf expected = new Quaternionf().rotationXYZ((float)Math.toRadians(x), (float)Math.toRadians(y), (float)Math.toRadians(z));
		Quaternionf actual = animation.calculateBoneTransformations("root", time, PInterpolationType.LINEAR, context).rotation();
		assertEquals(1, Math.abs(expected.dot(actual)), 0.00001f, "Euler " + x + ", " + y + ", " + z + " at " + time);
	}
}
