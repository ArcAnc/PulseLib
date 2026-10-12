/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PAnimationChannel
{
	public record Vector3fChannelType(ResourceLocation id, Vector3f defaultValue, boolean multiplicativeBlend)
			implements PAnimationChannelType<Vector3f>
	{
		@Override
		public Class<Vector3f> valueClass()
		{
			return Vector3f.class;
		}

		@Override
		public Vector3f defaultValue()
		{
			return new Vector3f(this.defaultValue);
		}

		@Override
		public void interpolate(Vector3f from, Vector3f to, float alpha, PInterpolation interpolation, Vector3f destination)
		{
			destination.set(from).lerp(to, transformedAlpha(alpha, interpolation));
		}

		@Override
		public void blend(Vector3f base, Vector3f layer, float weight, PBlendMode mode, Vector3f destination)
		{
			float clampedWeight = Math.clamp(weight, 0f, 1f);
			switch (mode)
			{
				case REPLACE -> destination.set(base).lerp(layer, clampedWeight);
				case ADDITIVE ->
				{
					if (this.multiplicativeBlend)
						destination.set(base).mul(new Vector3f(1f).lerp(layer, clampedWeight));
					else
						destination.set(base).add(new Vector3f(layer).mul(clampedWeight));
				}
			}
		}

		@Override
		public void apply(PPoseWriter pose, int boneIndex, Vector3f value)
		{
			if (this.multiplicativeBlend)
				pose.scale(boneIndex, value);
			else
				pose.translation(boneIndex, value);
		}
	}

	public record QuaternionChannelType(ResourceLocation id) implements PAnimationChannelType<Quaternionf>
	{
		@Override
		public Class<Quaternionf> valueClass()
		{
			return Quaternionf.class;
		}

		@Override
		public Quaternionf defaultValue()
		{
			return new Quaternionf();
		}

		@Override
		public void interpolate(Quaternionf from, Quaternionf to, float alpha, PInterpolation interpolation, Quaternionf destination)
		{
			destination.set(from).slerp(to, transformedAlpha(alpha, interpolation));
		}

		@Override
		public void blend(Quaternionf base, Quaternionf layer, float weight, PBlendMode mode, Quaternionf destination)
		{
			float clampedWeight = Math.clamp(weight, 0f, 1f);
			switch (mode)
			{
				case REPLACE -> destination.set(base).slerp(layer, clampedWeight);
				case ADDITIVE -> destination.set(base).mul(new Quaternionf().slerp(layer, clampedWeight));
			}
		}

		@Override
		public void apply(PPoseWriter pose, int boneIndex, Quaternionf value)
		{
			pose.rotation(boneIndex, value);
		}
	}

	private static float transformedAlpha(float alpha, PInterpolation interpolation)
	{
		return (float) interpolation.buildTransformer(Math.clamp(alpha, 0f, 1f));
	}
}
