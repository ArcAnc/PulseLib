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
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/animation/EasingType.java">EasingType</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 */
@FunctionalInterface
public interface PInterpolationType extends PInterpolation
{
	Map<String, PInterpolationType> INTERPOLATION_TYPES = new Object2ObjectOpenHashMap<>();
	
	PInterpolationType LINEAR = register("linear", PInterpolationType :: linear);
	PInterpolationType CATMULLROM = register("catmullrom", PInterpolationType :: catmullRom);
	PInterpolationType BEZIER = register("bezier", PInterpolationType :: bezier);
	PInterpolationType STEP = register("step", PInterpolationType :: step);
	
	double buildTransformer(double strength);
	
	static <T extends PInterpolationType> T register(String name, T interpolationType)
	{
		INTERPOLATION_TYPES.putIfAbsent(name, interpolationType);
		return interpolationType;
	}
	
	static double linear(double value)
	{
		return value;
	}
	
	static double step(double value)
	{
		return value < 1.0d ? 0.0d : 1.0d;
	}
	
	static double catmullRom(double value)
	{
		double t2 = value * value;
		double t3 = t2 * value;
		
		return 0.5d * (2 * value + (-1 + value) * t2 + (2 - value) * t3);
	}
	
	static double bezier(double value)
	{
		return bezier(value, 0.5d);
	}
	
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
