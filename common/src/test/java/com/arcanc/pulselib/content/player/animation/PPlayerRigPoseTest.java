/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PPlayerRigPoseTest
{
	@Test
	void capturedOrderAndOverrideUseEachConsumersVanillaBase()
	{
		var commands = new ArrayList<PPlayerRigPose.Contribution>();
		commands.add(contribution(PPlayerAnimationBlendMode.OVERRIDE, 0.5f, 1));
		commands.add(contribution(PPlayerAnimationBlendMode.OVERRIDE, 0.5f, 2));
		var frozen = new PPlayerRigPose(commands);
		commands.clear();
		var model = model();
		for (float baseline : new float[] {-7, 0, 13})
		{
			model.body.x = baseline;
			frozen.apply(model);
			assertEquals(baseline + 20, model.body.x, 0.00001f);
		}
	}

	@Test
	void allBlendModesReplayWeightedRotationAndScale()
	{
		for (var mode : PPlayerAnimationBlendMode.values())
			for (float weight : new float[] {0.25f, 0.6f, 1})
			{
				var model = model();
				model.body.x = 7;
				model.body.xRot = 0.2f;
				model.body.xScale = 2;
				var pose = new PPlayerBonePose(new Vector3f(1, 0, 0), new Quaternionf().rotationY(0.4f), new Vector3f(3, 1, 1), true, true, true);
				new PPlayerRigPose(List.of(new PPlayerRigPose.Contribution(PPlayerPart.BODY, pose, mode, weight))).apply(model);
				float delta = 16 * weight;
				float scale = 1 + 2 * weight;
				boolean difference = mode == PPlayerAnimationBlendMode.DIFFERENCE;
				boolean multiply = mode == PPlayerAnimationBlendMode.MULTIPLY_SCALE;
				assertEquals(multiply ? 7 : difference ? 7 - delta : 7 + delta, model.body.x, 0.00001f);
				assertEquals(difference ? 2 / scale : 2 * scale, model.body.xScale, 0.00001f);
				Quaternionf expected = new Quaternionf().rotationX(0.2f);
				if (mode == PPlayerAnimationBlendMode.OVERRIDE)
					expected.slerp(pose.rotation(), weight);
				else if (!multiply)
					expected.premul(new Quaternionf().rotationY(difference ? -0.4f * weight : 0.4f * weight));
				Quaternionf actual = new Quaternionf().rotationZYX(model.body.zRot, model.body.yRot, model.body.xRot);
				assertTrue(Math.abs(expected.dot(actual)) > 0.99999f, mode + " / " + weight);
			}
	}

	@Test
	void childOverlayInheritsParentWithoutAnotherContribution()
	{
		var model = model();
		model.jacket.x = 3;
		new PPlayerRigPose(List.of(contribution(PPlayerAnimationBlendMode.ADDITIVE_LOCAL, 1, 1))).apply(model);
		assertEquals(16, model.body.x, 0.00001f);
		assertEquals(3, model.jacket.x, 0.00001f);
	}

	private static PPlayerRigPose.Contribution contribution(PPlayerAnimationBlendMode mode, float weight, float x)
	{
		return new PPlayerRigPose.Contribution(PPlayerPart.BODY,
				new PPlayerBonePose(new Vector3f(x, 0, 0), new Quaternionf(), new Vector3f(1), true, false, false), mode, weight);
	}

	private static PlayerModel model()
	{
		return new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot(), false);
	}
}
