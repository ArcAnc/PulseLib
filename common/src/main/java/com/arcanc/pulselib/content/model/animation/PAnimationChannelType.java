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

public interface PAnimationChannelType<T>
{
	ResourceLocation id();

	Class<T> valueClass();

	T defaultValue();

	void interpolate(T from, T to, float alpha, PInterpolation interpolation, T destination);

	void blend(T base, T layer, float weight, PBlendMode mode, T destination);

	void apply(PPoseWriter pose, int boneIndex, T value);
}
