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

/**
 * Defines the value type, interpolation, blending and pose application of an animation channel.
 *
 * @param <T> the sampled animation channel value type
 */
public interface PAnimationChannelType<T>
{
	/**
	 * Returns the namespaced registry identifier of this channel type.
	 *
	 * @return the namespaced registry identifier of this channel type
	 */
	ResourceLocation id();

	/**
	 * Returns the runtime class accepted by this channel.
	 *
	 * @return the runtime class accepted by this channel
	 */
	Class<T> valueClass();

	/**
	 * Returns the neutral value used when a track does not contribute a value.
	 *
	 * @return the neutral value used when a track does not contribute a value
	 */
	T defaultValue();

	/**
	 * Interpolates adjacent keyframe values into the supplied destination.
	 *
	 * @param from the preceding value of the sampled segment
	 * @param to the following value of the sampled segment
	 * @param alpha the normalized interpolation progress
	 * @param interpolation the keyframe interpolation policy
	 * @param destination the mutable object receiving the result
	 */
	void interpolate(T from, T to, float alpha, PInterpolation interpolation, T destination);

	/**
	 * Optional four-point sampling. Existing custom channels keep their two-point
	 * interpolation; vector channels implement the built-in uniform Catmull-Rom.
	 *
	 * @param before the neighboring value before the segment start
	 * @param from the preceding value of the sampled segment
	 * @param to the following value of the sampled segment
	 * @param after the neighboring value after the segment end
	 * @param alpha the normalized interpolation progress
	 * @param interpolation the keyframe interpolation policy
	 * @param destination the mutable object receiving the result
	 */
	default void interpolate(T before, T from, T to, T after, float alpha, PInterpolation interpolation, T destination)
	{
		interpolate(from, to, alpha, interpolation, destination);
	}

	/**
	 * Combines a layer value with the accumulated channel value using the requested blend mode.
	 *
	 * @param base the accumulated channel value before the current layer
	 * @param layer the layer contribution being blended
	 * @param weight the contribution weight
	 * @param mode the rule used to combine the layer with the accumulated value
	 * @param destination the mutable object receiving the result
	 */
	void blend(T base, T layer, float weight, PBlendMode mode, T destination);

	/**
	 * Writes the sampled channel value into the target bone of a pose.
	 *
	 * @param pose the destination pose receiving sampled channel values
	 * @param boneIndex the zero-based baked-model bone index
	 * @param value the sampled animation channel value
	 */
	void apply(PPoseWriter pose, int boneIndex, T value);
}
