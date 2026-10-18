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

/**
 * Built-in vector and quaternion channel implementations for bone transform animation.
 */
public class PAnimationChannel
{
	/**
	 * Vector-valued animation channel with configurable default value, blend behavior and pose writer.
	 *
	 * @param id the namespaced resource or registration identifier
	 * @param defaultValue the default value
	 * @param multiplicativeBlend the multiplicative blend
	 */
	public record Vector3fChannelType(ResourceLocation id, Vector3f defaultValue, boolean multiplicativeBlend)
			implements PAnimationChannelType<Vector3f>
	{
		/**
		 * Returns the value class.
		 *
		 * @return the value class
		 */
		@Override
		public Class<Vector3f> valueClass()
		{
			return Vector3f.class;
		}

		/**
		 * Returns the default value.
		 *
		 * @return the default value
		 */
		@Override
		public Vector3f defaultValue()
		{
			return new Vector3f(this.defaultValue);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void interpolate(Vector3f from, Vector3f to, float alpha, PInterpolation interpolation, Vector3f destination)
		{
			destination.set(from).lerp(to, transformedAlpha(alpha, interpolation));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void interpolate(Vector3f before, Vector3f from, Vector3f to, Vector3f after,
		                        float alpha, PInterpolation interpolation, Vector3f destination)
		{
			if (interpolation != PInterpolationType.CATMULLROM)
			{
				interpolate(from, to, alpha, interpolation, destination);
				return;
			}
			destination.set(
					(float)PInterpolationType.catmullRom(alpha, before.x, from.x, to.x, after.x),
					(float)PInterpolationType.catmullRom(alpha, before.y, from.y, to.y, after.y),
					(float)PInterpolationType.catmullRom(alpha, before.z, from.z, to.z, after.z));
		}

		/**
		 * {@inheritDoc}
		 */
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

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void apply(PPoseWriter pose, int boneIndex, Vector3f value)
		{
			if (this.multiplicativeBlend)
				pose.scale(boneIndex, value);
			else
				pose.translation(boneIndex, value);
		}
	}

	/**
	 * Quaternion rotation channel that interpolates and blends bone orientations.
	 *
	 * @param id the namespaced resource or registration identifier
	 */
	public record QuaternionChannelType(ResourceLocation id) implements PAnimationChannelType<Quaternionf>
	{
		/**
		 * Returns the value class.
		 *
		 * @return the value class
		 */
		@Override
		public Class<Quaternionf> valueClass()
		{
			return Quaternionf.class;
		}

		/**
		 * Returns the default value.
		 *
		 * @return the default value
		 */
		@Override
		public Quaternionf defaultValue()
		{
			return new Quaternionf();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void interpolate(Quaternionf from, Quaternionf to, float alpha, PInterpolation interpolation, Quaternionf destination)
		{
			destination.set(from).slerp(to, transformedAlpha(alpha, interpolation));
		}

		/**
		 * {@inheritDoc}
		 */
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

		/**
		 * {@inheritDoc}
		 */
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
