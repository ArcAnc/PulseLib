/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Provides support for animation channel.
 */
public class PAnimationChannel
{
/**
 * Immutable value object representing vector3f channel type.
 */
	public record Vector3fChannelType(Identifier id, Vector3f defaultValue, boolean multiplicativeBlend)
			implements PAnimationChannelType<Vector3f>
	{
		/**
		 * Performs the value class operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public Class<Vector3f> valueClass()
		{
			return Vector3f.class;
		}

		/**
		 * Performs the default value operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public Vector3f defaultValue()
		{
			return new Vector3f(this.defaultValue);
		}

		/**
		 * Performs the interpolate operation.
		 * @param from the from to use.
		 * @param to the to to use.
		 * @param alpha the alpha to use.
		 * @param interpolation the interpolation to use.
		 * @param destination the destination to use.
		 */
		@Override
		public void interpolate(Vector3f from, Vector3f to, float alpha, PInterpolation interpolation, Vector3f destination)
		{
			destination.set(from).lerp(to, transformedAlpha(alpha, interpolation));
		}

		/**
		 * Performs the blend operation.
		 * @param base the base to use.
		 * @param layer the layer to use.
		 * @param weight the weight to use.
		 * @param mode the mode to use.
		 * @param destination the destination to use.
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
		 * Performs the apply operation.
		 * @param pose the pose to use.
		 * @param boneIndex the bone index to use.
		 * @param value the value to use.
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
 * Immutable value object representing quaternion channel type.
 */
	public record QuaternionChannelType(Identifier id) implements PAnimationChannelType<Quaternionf>
	{
		/**
		 * Performs the value class operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public Class<Quaternionf> valueClass()
		{
			return Quaternionf.class;
		}

		/**
		 * Performs the default value operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public Quaternionf defaultValue()
		{
			return new Quaternionf();
		}

		/**
		 * Performs the interpolate operation.
		 * @param from the from to use.
		 * @param to the to to use.
		 * @param alpha the alpha to use.
		 * @param interpolation the interpolation to use.
		 * @param destination the destination to use.
		 */
		@Override
		public void interpolate(Quaternionf from, Quaternionf to, float alpha, PInterpolation interpolation, Quaternionf destination)
		{
			destination.set(from).slerp(to, transformedAlpha(alpha, interpolation));
		}

		/**
		 * Performs the blend operation.
		 * @param base the base to use.
		 * @param layer the layer to use.
		 * @param weight the weight to use.
		 * @param mode the mode to use.
		 * @param destination the destination to use.
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
		 * Performs the apply operation.
		 * @param pose the pose to use.
		 * @param boneIndex the bone index to use.
		 * @param value the value to use.
		 */
		@Override
		public void apply(PPoseWriter pose, int boneIndex, Quaternionf value)
		{
			pose.rotation(boneIndex, value);
		}
	}

	/**
	 * Performs the transformed alpha operation.
	 * @param alpha the alpha to use.
	 * @param interpolation the interpolation to use.
	 * @return the value produced by this operation.
	 */
	private static float transformedAlpha(float alpha, PInterpolation interpolation)
	{
		return (float) interpolation.buildTransformer(Math.clamp(alpha, 0f, 1f));
	}
}
