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

public interface PAnimationChannelType<T>
{
	/**
	 * Performs the id operation.
	 * @return the value produced by this operation.
	 */
	Identifier id();

	/**
	 * Performs the value class operation.
	 * @return the value produced by this operation.
	 */
	Class<T> valueClass();

	/**
	 * Performs the default value operation.
	 * @return the value produced by this operation.
	 */
	T defaultValue();

	/**
	 * Performs the interpolate operation.
	 * @param from the from to use.
	 * @param to the to to use.
	 * @param alpha the alpha to use.
	 * @param interpolation the interpolation to use.
	 * @param destination the destination to use.
	 */
	void interpolate(T from, T to, float alpha, PInterpolation interpolation, T destination);

	/**
	 * Performs the blend operation.
	 * @param base the base to use.
	 * @param layer the layer to use.
	 * @param weight the weight to use.
	 * @param mode the mode to use.
	 * @param destination the destination to use.
	 */
	void blend(T base, T layer, float weight, PBlendMode mode, T destination);

	/**
	 * Performs the apply operation.
	 * @param pose the pose to use.
	 * @param boneIndex the bone index to use.
	 * @param value the value to use.
	 */
	void apply(PPoseWriter pose, int boneIndex, T value);
}
