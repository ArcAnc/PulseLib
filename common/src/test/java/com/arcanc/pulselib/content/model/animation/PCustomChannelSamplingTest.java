/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.data.gecko.MolangParser;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PCustomChannelSamplingTest
{
	@Test
	void existingCustomChannelKeepsItsTwoPointInterpolationThroughTheNewSplineSampler()
	{
		LegacyChannel channel = new LegacyChannel();
		var track = new PAnimationTrack<>(channel, List.of(key(0, 0), key(10, 1), key(20, 1), key(30, 0)));
		Vector3f sampled = PAnimationTrackSampler.sample(track, 15, new PAnimationEvaluationContext(new MolangParser.Context()));
		assertEquals(1.0f, sampled.x);
		assertEquals(1, channel.calls);
		PPose pose = new PPose(1);
		channel.apply(pose, 0, sampled);
		assertEquals(1.0f, pose.translation(0).x);
	}

	private static PKeyframe<Vector3f> key(float time, float scalar)
	{
		PAnimationValue<Vector3f> value = (context, destination) -> destination.set(scalar, 0, 0);
		return new PKeyframe<>(time, value, value, PInterpolationType.CATMULLROM);
	}

	/** Emulates a downstream channel implementing the original two-point public contract. */
	private static final class LegacyChannel implements PAnimationChannelType<Vector3f>
	{
		private final PAnimationChannel.Vector3fChannelType delegate = new PAnimationChannel.Vector3fChannelType(
				Identifier.fromNamespaceAndPath("pulselibtest", "legacy_channel"), new Vector3f(), false);
		private int calls;

		@Override
		public Identifier id()
		{
			return this.delegate.id();
		}

		@Override
		public Class<Vector3f> valueClass()
		{
			return Vector3f.class;
		}

		@Override
		public Vector3f defaultValue()
		{
			return this.delegate.defaultValue();
		}

		@Override
		public void interpolate(Vector3f from, Vector3f to, float alpha, PInterpolation interpolation, Vector3f destination)
		{
			this.calls++;
			this.delegate.interpolate(from, to, alpha, interpolation, destination);
		}

		@Override
		public void blend(Vector3f base, Vector3f layer, float weight, PBlendMode mode, Vector3f destination)
		{
			this.delegate.blend(base, layer, weight, mode, destination);
		}

		@Override
		public void apply(PPoseWriter pose, int boneIndex, Vector3f value)
		{
			pose.translation(boneIndex, value);
		}
	}
}
