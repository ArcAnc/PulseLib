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

/**
 * Defines the contract for animation state.
 */
public sealed interface PAnimationState permits PAnimationState.Clip, PAnimationState.BlendSpace1D,
		PAnimationState.BlendSpace2D, PAnimationState.OneShotOverlay
{
	/**
	 * Performs the name operation.
	 * @return the value produced by this operation.
	 */
	String name();

	/**
	 * Performs the samples operation.
	 * @param parameters the parameters to use.
	 * @return the value produced by this operation.
	 */
	List<PAnimationSample> samples(PAnimationParameters parameters);

	/**
	 * Performs the animation type operation.
	 * @return the value produced by this operation.
	 */
	PAnimationType animationType();

	/**
	 * Performs the interpolation operation.
	 * @return the value produced by this operation.
	 */
	PInterpolationType interpolation();

	/**
	 * Performs the speed operation.
	 * @return the value produced by this operation.
	 */
	float speed();

	/**
	 * Performs the synchronized cycle operation.
	 * @return the value produced by this operation.
	 */
	boolean synchronizedCycle();

	/**
	 * Determines whether overlay.
	 * @return the value produced by this operation.
	 */
	default boolean isOverlay()
	{
		return false;
	}

/**
 * Immutable value object representing clip.
 */
	record Clip(String name, String animation, PAnimationType animationType, PInterpolationType interpolation,
	            float speed, boolean synchronizedCycle) implements PAnimationState
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 * @param animation the animation to use.
		 */
		public Clip(String name, String animation)
		{
			this(name, animation, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, false);
		}

		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 * @param animation the animation to use.
		 * @param animationType the animation type to use.
		 * @param interpolation the interpolation to use.
		 * @param speed the speed to use.
		 * @param synchronizedCycle the synchronized cycle to use.
		 */
		public Clip
		{
			validate(name, animationType, interpolation, speed);
			animation = require(animation, "Animation name");
		}

		/**
		 * Performs the samples operation.
		 * @param parameters the parameters to use.
		 * @return the value produced by this operation.
		 */
		@Override
		public List<PAnimationSample> samples(PAnimationParameters parameters)
		{
			return List.of(new PAnimationSample(this.animation, 1.0f));
		}
	}

/**
 * Immutable value object representing blend space1 d.
 */
	record BlendSpace1D(String name, String parameter, List<Point> points, PAnimationType animationType,
	                  PInterpolationType interpolation, float speed, boolean synchronizedCycle) implements PAnimationState
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 * @param parameter the parameter to use.
		 * @param points the points to use.
		 */
		public BlendSpace1D(String name, String parameter, List<Point> points)
		{
			this(name, parameter, points, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, true);
		}

		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 * @param parameter the parameter to use.
		 * @param points the points to use.
		 * @param animationType the animation type to use.
		 * @param interpolation the interpolation to use.
		 * @param speed the speed to use.
		 * @param synchronizedCycle the synchronized cycle to use.
		 */
		public BlendSpace1D
		{
			validate(name, animationType, interpolation, speed);
			parameter = require(parameter, "Blend-space parameter");
			points = sortedPoints(points);
		}

		/**
		 * Performs the samples operation.
		 * @param parameters the parameters to use.
		 * @return the value produced by this operation.
		 */
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

/**
 * Immutable value object representing point.
 */
		public record Point(float coordinate, String animation)
		{
			/**
			 * Creates an instance of the enclosing type.
			 * @param coordinate the coordinate to use.
			 * @param animation the animation to use.
			 */
			public Point
			{
				animation = require(animation, "Animation name");
			}
		}
	}

/**
 * Immutable value object representing blend space2 d.
 */
	record BlendSpace2D(String name, String xParameter, String yParameter, List<Point> points,
	                  PAnimationType animationType, PInterpolationType interpolation, float speed,
	                  boolean synchronizedCycle) implements PAnimationState
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 * @param xParameter the x parameter to use.
		 * @param yParameter the y parameter to use.
		 * @param points the points to use.
		 */
		public BlendSpace2D(String name, String xParameter, String yParameter, List<Point> points)
		{
			this(name, xParameter, yParameter, points, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, true);
		}

		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 * @param xParameter the x parameter to use.
		 * @param yParameter the y parameter to use.
		 * @param points the points to use.
		 * @param animationType the animation type to use.
		 * @param interpolation the interpolation to use.
		 * @param speed the speed to use.
		 * @param synchronizedCycle the synchronized cycle to use.
		 */
		public BlendSpace2D
		{
			validate(name, animationType, interpolation, speed);
			xParameter = require(xParameter, "X blend-space parameter");
			yParameter = require(yParameter, "Y blend-space parameter");
			if (points == null || points.isEmpty())
				throw new IllegalArgumentException("A 2D blend space needs at least one point");
			points = List.copyOf(points);
		}

		/**
		 * Performs the samples operation.
		 * @param parameters the parameters to use.
		 * @return the value produced by this operation.
		 */
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

/**
 * Immutable value object representing point.
 */
		public record Point(float x, float y, String animation)
		{
			/**
			 * Creates an instance of the enclosing type.
			 * @param x the x to use.
			 * @param y the y to use.
			 * @param animation the animation to use.
			 */
			public Point
			{
				animation = require(animation, "Animation name");
			}
		}
	}
	
/**
 * Immutable value object representing one shot overlay.
 */
	record OneShotOverlay(String name, String trigger, String animation, float fadeInDuration, float fadeOutDuration,
	                     PInterpolationType interpolation, float speed, boolean synchronizedCycle) implements PAnimationState
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 * @param trigger the trigger to use.
		 * @param animation the animation to use.
		 */
		public OneShotOverlay(String name, String trigger, String animation)
		{
			this(name, trigger, animation, 0.0f, 0.0f, PInterpolationType.LINEAR, 1.0f, false);
		}

		/**
		 * Creates an instance of the enclosing type.
		 * @param name the name to use.
		 * @param trigger the trigger to use.
		 * @param animation the animation to use.
		 * @param fadeInDuration the fade in duration to use.
		 * @param fadeOutDuration the fade out duration to use.
		 * @param interpolation the interpolation to use.
		 * @param speed the speed to use.
		 * @param synchronizedCycle the synchronized cycle to use.
		 */
		public OneShotOverlay
		{
			validate(name, PAnimationType.PLAY_ONCE, interpolation, speed);
			trigger = require(trigger, "Overlay trigger");
			animation = require(animation, "Animation name");
			if (fadeInDuration < 0.0f || fadeOutDuration < 0.0f)
				throw new IllegalArgumentException("Overlay fade durations must be non-negative");
		}

		/**
		 * Performs the samples operation.
		 * @param parameters the parameters to use.
		 * @return the value produced by this operation.
		 */
		@Override
		public List<PAnimationSample> samples(PAnimationParameters parameters)
		{
			return List.of(new PAnimationSample(this.animation, 1.0f));
		}

		/**
		 * Performs the animation type operation.
		 * @return the value produced by this operation.
		 */
		@Override
		public PAnimationType animationType()
		{
			return PAnimationType.PLAY_ONCE;
		}

		/**
		 * Determines whether overlay.
		 * @return the value produced by this operation.
		 */
		@Override
		public boolean isOverlay()
		{
			return true;
		}
	}

	/**
	 * Performs the sorted points operation.
	 * @param points the points to use.
	 * @return the value produced by this operation.
	 */
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

	/**
	 * Performs the validate operation.
	 * @param name the name to use.
	 * @param type the type to use.
	 * @param interpolation the interpolation to use.
	 * @param speed the speed to use.
	 */
	private static void validate(String name, PAnimationType type, PInterpolationType interpolation, float speed)
	{
		require(name, "State name");
		Objects.requireNonNull(type);
		Objects.requireNonNull(interpolation);
		if (speed < 0.0f)
			throw new IllegalArgumentException("Animation state speed must be non-negative");
	}

	/**
	 * Performs the require operation.
	 * @param value the value to use.
	 * @param what the what to use.
	 * @return the value produced by this operation.
	 */
	private static String require(String value, String what)
	{
		if (value == null || value.isBlank())
			throw new IllegalArgumentException(what + " must not be blank");
		return value;
	}

	/**
	 * Performs the square operation.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	private static float square(float value)
	{
		return value * value;
	}
}
