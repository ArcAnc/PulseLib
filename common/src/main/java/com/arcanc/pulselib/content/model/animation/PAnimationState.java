/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public sealed interface PAnimationState permits PAnimationState.Clip, PAnimationState.BlendSpace1D,
		PAnimationState.BlendSpace2D, PAnimationState.OneShotOverlay
{
	String name();

	List<PAnimationSample> samples(PAnimationParameters parameters);

	PAnimationType animationType();

	PInterpolationType interpolation();

	float speed();

	boolean synchronizedCycle();

	default boolean isOverlay()
	{
		return false;
	}

	record Clip(String name, String animation, PAnimationType animationType, PInterpolationType interpolation,
	            float speed, boolean synchronizedCycle) implements PAnimationState
	{
		public Clip(String name, String animation)
		{
			this(name, animation, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, false);
		}

		public Clip
		{
			validate(name, animationType, interpolation, speed);
			animation = require(animation, "Animation name");
		}

		@Override
		public List<PAnimationSample> samples(PAnimationParameters parameters)
		{
			return List.of(new PAnimationSample(this.animation, 1.0f));
		}
	}

	record BlendSpace1D(String name, String parameter, List<Point> points, PAnimationType animationType,
	                  PInterpolationType interpolation, float speed, boolean synchronizedCycle) implements PAnimationState
	{
		public BlendSpace1D(String name, String parameter, List<Point> points)
		{
			this(name, parameter, points, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, true);
		}

		public BlendSpace1D
		{
			validate(name, animationType, interpolation, speed);
			parameter = require(parameter, "Blend-space parameter");
			points = sortedPoints(points);
		}

		@Override
		public List<PAnimationSample> samples(PAnimationParameters parameters)
		{
			float value = parameters.get(this.parameter);
			if (value <= this.points.getFirst().coordinate())
				return List.of(new PAnimationSample(this.points.getFirst().animation(), 1.0f));
			Point last = this.points.getLast();
			if (value >= last.coordinate())
				return List.of(new PAnimationSample(last.animation(), 1.0f));
			for (int index = 1; index < this.points.size(); index++)
			{
				Point right = this.points.get(index);
				if (value > right.coordinate())
					continue;
				Point left = this.points.get(index - 1);
				float alpha = (value - left.coordinate()) / (right.coordinate() - left.coordinate());
				return List.of(new PAnimationSample(left.animation(), 1.0f - alpha), new PAnimationSample(right.animation(), alpha));
			}
			return List.of();
		}

		public record Point(float coordinate, String animation)
		{
			public Point
			{
				animation = require(animation, "Animation name");
			}
		}
	}

	record BlendSpace2D(String name, String xParameter, String yParameter, List<Point> points,
	                  PAnimationType animationType, PInterpolationType interpolation, float speed,
	                  boolean synchronizedCycle) implements PAnimationState
	{
		public BlendSpace2D(String name, String xParameter, String yParameter, List<Point> points)
		{
			this(name, xParameter, yParameter, points, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, true);
		}

		public BlendSpace2D
		{
			validate(name, animationType, interpolation, speed);
			xParameter = require(xParameter, "X blend-space parameter");
			yParameter = require(yParameter, "Y blend-space parameter");
			if (points == null || points.isEmpty())
				throw new IllegalArgumentException("A 2D blend space needs at least one point");
			points = List.copyOf(points);
		}

		@Override
		public List<PAnimationSample> samples(PAnimationParameters parameters)
		{
			float x = parameters.get(this.xParameter);
			float y = parameters.get(this.yParameter);
			List<PAnimationSample> result = new ArrayList<>(this.points.size());
			float total = 0.0f;
			for (Point point : this.points)
			{
				float distanceSquared = square(x - point.x()) + square(y - point.y());
				if (distanceSquared < 1.0e-8f)
					return List.of(new PAnimationSample(point.animation(), 1.0f));
				float weight = 1.0f / distanceSquared;
				result.add(new PAnimationSample(point.animation(), weight));
				total += weight;
			}
			float inverse = 1.0f / total;
			return result.stream().map(sample -> new PAnimationSample(sample.animation(), sample.weight() * inverse)).toList();
		}

		public record Point(float x, float y, String animation)
		{
			public Point
			{
				animation = require(animation, "Animation name");
			}
		}
	}
	
	record OneShotOverlay(String name, String trigger, String animation, float fadeInDuration, float fadeOutDuration,
	                     PInterpolationType interpolation, float speed, boolean synchronizedCycle) implements PAnimationState
	{
		public OneShotOverlay(String name, String trigger, String animation)
		{
			this(name, trigger, animation, 0.0f, 0.0f, PInterpolationType.LINEAR, 1.0f, false);
		}

		public OneShotOverlay
		{
			validate(name, PAnimationType.PLAY_ONCE, interpolation, speed);
			trigger = require(trigger, "Overlay trigger");
			animation = require(animation, "Animation name");
			if (fadeInDuration < 0.0f || fadeOutDuration < 0.0f)
				throw new IllegalArgumentException("Overlay fade durations must be non-negative");
		}

		@Override
		public List<PAnimationSample> samples(PAnimationParameters parameters)
		{
			return List.of(new PAnimationSample(this.animation, 1.0f));
		}

		@Override
		public PAnimationType animationType()
		{
			return PAnimationType.PLAY_ONCE;
		}

		@Override
		public boolean isOverlay()
		{
			return true;
		}
	}

	private static List<BlendSpace1D.Point> sortedPoints(List<BlendSpace1D.Point> points)
	{
		if (points == null || points.isEmpty())
			throw new IllegalArgumentException("A 1D blend space needs at least one point");
		List<BlendSpace1D.Point> sorted = points.stream().sorted(Comparator.comparingDouble(BlendSpace1D.Point::coordinate)).toList();
		for (int index = 1; index < sorted.size(); index++)
			if (sorted.get(index - 1).coordinate() == sorted.get(index).coordinate())
				throw new IllegalArgumentException("Blend-space point coordinates must be unique");
		return sorted;
	}

	private static void validate(String name, PAnimationType type, PInterpolationType interpolation, float speed)
	{
		require(name, "State name");
		Objects.requireNonNull(type);
		Objects.requireNonNull(interpolation);
		if (speed < 0.0f)
			throw new IllegalArgumentException("Animation state speed must be non-negative");
	}

	private static String require(String value, String what)
	{
		if (value == null || value.isBlank())
			throw new IllegalArgumentException(what + " must not be blank");
		return value;
	}

	private static float square(float value)
	{
		return value * value;
	}
}
