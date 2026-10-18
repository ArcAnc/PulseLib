/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

/**
 * Named animation interpolation policies and their registration helpers.
 * <p>
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/animation/EasingType.java">EasingType</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 *

 */
@FunctionalInterface
public interface PInterpolationType extends PInterpolation
{
	/**
	 * Interpolation policies indexed by their serialized names.
	 */
	Map<String, PInterpolationType> INTERPOLATION_TYPES = new Object2ObjectOpenHashMap<>();
	
	/**
	 * Linear interpolation between adjacent keyframes.
	 */
	PInterpolationType LINEAR = register("linear", PInterpolationType :: linear);
	/**
	 * Cubic interpolation using adjacent and neighboring keyframe values.
	 */
	PInterpolationType CATMULLROM = register("catmullrom", PInterpolationType :: catmullRom);
	/**
	 * Bezier easing between adjacent keyframe values.
	 */
	PInterpolationType BEZIER = register("bezier", PInterpolationType :: bezier);
	/**
	 * Holds the preceding keyframe value until the next keyframe boundary.
	 */
	PInterpolationType STEP = register("step", PInterpolationType :: step);
	
	/**
	 * Transforms normalized keyframe progress with this interpolation policy.
	 *
	 * @param strength the strength
	 * @return the transformed interpolation weight
	 */
	double buildTransformer(double strength);
	
	/**
	 * Registers a named interpolation policy and returns it.
	 *
	 * @param <T> the value type carried by this contract
	 * @param name the interpolation policy name
	 * @param interpolationType the keyframe interpolation policy
	 * @return the registered interpolation policy
	 */
	static <T extends PInterpolationType> T register(String name, T interpolationType)
	{
		INTERPOLATION_TYPES.putIfAbsent(name, interpolationType);
		return interpolationType;
	}
	
	/**
	 * Returns unchanged progress for linear interpolation.
	 *
	 * @param value the normalized interpolation or easing progress
	 * @return unchanged progress for linear interpolation
	 */
	static double linear(double value)
	{
		return value;
	}
	
	/**
	 * Returns the step interpolation weight for the supplied progress.
	 *
	 * @param value the normalized interpolation or easing progress
	 * @return the step interpolation weight for the supplied progress
	 */
	static double step(double value)
	{
		return value < 1.0d ? 0.0d : 1.0d;
	}
	
	/**
	 * Two-point compatibility transformer: collinear, equally spaced anchors
	 * (-1, 0, 1, 2) reduce to linear alpha. Keyframe sampling uses four points.
	 *
	 * @param value the normalized interpolation or easing progress
	 * @return the cubic interpolation value
	 */
	static double catmullRom(double value)
	{
		return value;
	}

	/**
	 * Uniform Catmull-Rom over P1/P2, with P0/P3 as tangent anchors.
	 *
	 * @param value the normalized interpolation or easing progress
	 * @param p0 the neighboring sample before the segment
	 * @param p1 the segment start sample
	 * @param p2 the segment end sample
	 * @param p3 the neighboring sample after the segment
	 * @return the cubic interpolation value
	 */
	static double catmullRom(double value, double p0, double p1, double p2, double p3)
	{
		if (value == 0d)
		{
			return p1;
		}
		if (value == 1d)
		{
			return p2;
		}
		double t2 = value * value;
		double t3 = t2 * value;
		return 0.5d * (2d * p1 + (p2 - p0) * value +
				(2d * p0 - 5d * p1 + 4d * p2 - p3) * t2 +
				(-p0 + 3d * p1 - 3d * p2 + p3) * t3);
	}

	/**
	 * Evaluates a cubic Bezier easing weight for the supplied progress.
	 *
	 * @param value the normalized interpolation or easing progress
	 * @return the Bezier easing weight
	 */
	static double bezier(double value)
	{
		return bezier(value, 0.5d);
	}
	
	/**
	 * Evaluates a cubic Bezier easing weight for the supplied progress.
	 *
	 * @param value the normalized interpolation or easing progress
	 * @param strength the strength
	 * @return the Bezier easing weight
	 */
	static double bezier(double value, double strength)
	{
		value = Math.clamp(value, 0.0d, 1.0d);
		strength = Math.clamp(strength, 0.0d, 1.0d);
		
		double u = 1.0d - value;
		
		double p0 = 0.0d;
		double p1 = strength;
		double p2 = 1.0d - strength;
		double p3 = 1.0d;
		
		return u * u * u * p0 +
				3.0d * u * u * value * p1 +
				3.0d * u * value * value * p2 +
				value * value * value * p3;
	}
}
