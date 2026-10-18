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
 * Named graph state that selects weighted clips and defines their playback policy.
 */
public sealed interface PAnimationState permits PAnimationState.Clip, PAnimationState.BlendSpace1D,
		PAnimationState.BlendSpace2D, PAnimationState.OneShotOverlay
{
	/**
	 * Returns the unique name used to identify this graph state.
	 *
	 * @return the unique name used to identify this graph state
	 */
	String name();

	/**
	 * Selects weighted animation clips from the current graph parameter values.
	 *
	 * @param parameters the graph parameter and trigger state
	 * @return the animation clips and their contribution weights
	 */
	List<PAnimationSample> samples(PAnimationParameters parameters);

	/**
	 * Returns the loop or completion policy of this state.
	 *
	 * @return the loop or completion policy of this state
	 */
	PAnimationType animationType();

	/**
	 * Returns the interpolation policy used to sample this state.
	 *
	 * @return the interpolation policy used to sample this state
	 */
	PInterpolationType interpolation();

	/**
	 * Returns the multiplier applied to this state's playback clock.
	 *
	 * @return the multiplier applied to this state's playback clock
	 */
	float speed();

	/**
	 * Returns whether transitions should preserve the normalized cycle phase.
	 *
	 * @return whether transitions should preserve the normalized cycle phase
	 */
	boolean synchronizedCycle();

	/**
	 * Returns whether this state contributes a one-shot overlay instead of a base-state pose.
	 *
	 * @return whether this state contributes a one-shot overlay instead of a base-state pose
	 */
	default boolean isOverlay()
	{
		return false;
	}

	/**
	 * Graph state that samples one named animation clip.
	 *
	 * @param name the graph state name
	 * @param animation the source animation clip name
	 * @param animationType the animation loop and completion policy
	 * @param interpolation the keyframe interpolation policy
	 * @param speed the playback speed multiplier
	 * @param synchronizedCycle whether cyclic playback preserves phase across state changes
	 */
	record Clip(String name, String animation, PAnimationType animationType, PInterpolationType interpolation,
	            float speed, boolean synchronizedCycle) implements PAnimationState
	{
		/**
		 * Creates a graph state that samples the supplied animation clip.
		 *
		 * @param name the graph state name
		 * @param animation the source animation clip name
		 */
		public Clip(String name, String animation)
		{
			this(name, animation, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, false);
		}

		/**
		 * Creates a graph state that samples the supplied animation clip.
		 *
		 * @param name the graph state name
		 * @param animation the source animation clip name
		 * @param animationType the animation loop and completion policy
		 * @param interpolation the keyframe interpolation policy
		 * @param speed the playback speed multiplier
		 * @param synchronizedCycle whether cyclic playback preserves phase across state changes
		 */
		public Clip
		{
			validate(name, animationType, interpolation, speed);
			animation = require(animation, "Animation name");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<PAnimationSample> samples(PAnimationParameters parameters)
		{
			return List.of(new PAnimationSample(this.animation, 1.0f));
		}
	}

	/**
	 * Graph state that blends neighboring clips along one numeric parameter.
	 *
	 * @param name the graph state name
	 * @param parameter the graph parameter selecting the blend-space coordinate
	 * @param points the clips and coordinates defining the blend space
	 * @param animationType the animation loop and completion policy
	 * @param interpolation the keyframe interpolation policy
	 * @param speed the playback speed multiplier
	 * @param synchronizedCycle whether cyclic playback preserves phase across state changes
	 */
	record BlendSpace1D(String name, String parameter, List<Point> points, PAnimationType animationType,
	                  PInterpolationType interpolation, float speed, boolean synchronizedCycle) implements PAnimationState
	{
		/**
		 * Creates a graph state that blends clips along one named numeric parameter.
		 *
		 * @param name the graph state name
		 * @param parameter the graph parameter selecting the blend-space coordinate
		 * @param points the clips and coordinates defining the blend space
		 */
		public BlendSpace1D(String name, String parameter, List<Point> points)
		{
			this(name, parameter, points, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, true);
		}

		/**
		 * Creates a graph state that blends clips along one named numeric parameter.
		 *
		 * @param name the graph state name
		 * @param parameter the graph parameter selecting the blend-space coordinate
		 * @param points the clips and coordinates defining the blend space
		 * @param animationType the animation loop and completion policy
		 * @param interpolation the keyframe interpolation policy
		 * @param speed the playback speed multiplier
		 * @param synchronizedCycle whether cyclic playback preserves phase across state changes
		 */
		public BlendSpace1D
		{
			validate(name, animationType, interpolation, speed);
			parameter = require(parameter, "Blend-space parameter");
			points = sortedPoints(points);
		}

		/**
		 * {@inheritDoc}
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
		 * Animation clip located at one coordinate of a one-dimensional blend space.
		 *
		 * @param coordinate the coordinate
		 * @param animation the source animation clip name
		 */
		public record Point(float coordinate, String animation)
		{
			/**
			 * Associates an animation clip with a one-dimensional blend coordinate.
			 *
			 * @param coordinate the coordinate
			 * @param animation the source animation clip name
			 */
			public Point
			{
				animation = require(animation, "Animation name");
			}
		}
	}

	/**
	 * Graph state that blends clips from two numeric graph parameters.
	 *
	 * @param name the graph state name
	 * @param xParameter the graph parameter selecting the horizontal blend coordinate
	 * @param yParameter the graph parameter selecting the vertical blend coordinate
	 * @param points the clips and coordinates defining the blend space
	 * @param animationType the animation loop and completion policy
	 * @param interpolation the keyframe interpolation policy
	 * @param speed the playback speed multiplier
	 * @param synchronizedCycle whether cyclic playback preserves phase across state changes
	 */
	record BlendSpace2D(String name, String xParameter, String yParameter, List<Point> points,
	                  PAnimationType animationType, PInterpolationType interpolation, float speed,
	                  boolean synchronizedCycle) implements PAnimationState
	{
		/**
		 * Creates a graph state that blends clips across two named numeric parameters.
		 *
		 * @param name the graph state name
		 * @param xParameter the graph parameter selecting the horizontal blend coordinate
		 * @param yParameter the graph parameter selecting the vertical blend coordinate
		 * @param points the clips and coordinates defining the blend space
		 */
		public BlendSpace2D(String name, String xParameter, String yParameter, List<Point> points)
		{
			this(name, xParameter, yParameter, points, PAnimationType.CYCLE, PInterpolationType.LINEAR, 1.0f, true);
		}

		/**
		 * Creates a graph state that blends clips across two named numeric parameters.
		 *
		 * @param name the graph state name
		 * @param xParameter the graph parameter selecting the horizontal blend coordinate
		 * @param yParameter the graph parameter selecting the vertical blend coordinate
		 * @param points the clips and coordinates defining the blend space
		 * @param animationType the animation loop and completion policy
		 * @param interpolation the keyframe interpolation policy
		 * @param speed the playback speed multiplier
		 * @param synchronizedCycle whether cyclic playback preserves phase across state changes
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
		 * {@inheritDoc}
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
		 * Animation clip located at a two-dimensional blend-space coordinate.
		 *
		 * @param x the x component or coordinate
		 * @param y the y component or coordinate
		 * @param animation the source animation clip name
		 */
		public record Point(float x, float y, String animation)
		{
			/**
			 * Associates an animation clip with a two-dimensional blend coordinate.
			 *
			 * @param x the x component or coordinate
			 * @param y the y component or coordinate
			 * @param animation the source animation clip name
			 */
			public Point
			{
				animation = require(animation, "Animation name");
			}
		}
	}
	
	/**
	 * Immutable overlay definition with a signed playback speed. Positive starts at the beginning,
	 * negative starts at the clip end, and zero pauses. Runtime speed changes never seek the clip.
	 *
	 * @param name the graph state name
	 * @param trigger the trigger name that activates the overlay
	 * @param animation the source animation clip name
	 * @param fadeInDuration the overlay fade-in duration in game ticks
	 * @param fadeOutDuration the overlay fade-out duration in game ticks
	 * @param interpolation the keyframe interpolation policy
	 * @param speed the playback speed multiplier
	 * @param synchronizedCycle whether cyclic playback preserves phase across state changes
	 */
	record OneShotOverlay(String name, String trigger, String animation, float fadeInDuration, float fadeOutDuration,
	                     PInterpolationType interpolation, float speed, boolean synchronizedCycle) implements PAnimationState
	{
		/**
		 * Creates a trigger-driven overlay with the supplied fade and playback settings.
		 *
		 * @param name the graph state name
		 * @param trigger the trigger name that activates the overlay
		 * @param animation the source animation clip name
		 */
		public OneShotOverlay(String name, String trigger, String animation)
		{
			this(name, trigger, animation, 0.0f, 0.0f, PInterpolationType.LINEAR, 1.0f, false);
		}

		/**
		 * Creates a trigger-driven overlay with the supplied fade and playback settings.
		 *
		 * @param name the graph state name
		 * @param trigger the trigger name that activates the overlay
		 * @param animation the source animation clip name
		 * @param fadeInDuration the overlay fade-in duration in game ticks
		 * @param fadeOutDuration the overlay fade-out duration in game ticks
		 * @param interpolation the keyframe interpolation policy
		 * @param speed the playback speed multiplier
		 * @param synchronizedCycle whether cyclic playback preserves phase across state changes
		 */
		public OneShotOverlay
		{
			validate(name, PAnimationType.PLAY_ONCE, interpolation);
			trigger = require(trigger, "Overlay trigger");
			animation = require(animation, "Animation name");
			if (!Float.isFinite(speed))
			{
				throw new IllegalArgumentException("Overlay speed must be finite");
			}
			if (!Float.isFinite(fadeInDuration) || !Float.isFinite(fadeOutDuration) || fadeInDuration < 0.0f || fadeOutDuration < 0.0f)
			{
				throw new IllegalArgumentException("Overlay fade durations must be finite and non-negative");
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<PAnimationSample> samples(PAnimationParameters parameters)
		{
			return List.of(new PAnimationSample(this.animation, 1.0f));
		}

		/**
		 * Returns the animation loop and completion policy.
		 *
		 * @return the animation loop and completion policy
		 */
		@Override
		public PAnimationType animationType()
		{
			return PAnimationType.PLAY_ONCE;
		}

		/**
		 * Returns the is overlay.
		 *
		 * @return the is overlay
		 */
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
		validate(name, type, interpolation);
		if (speed < 0.0f)
		{
			throw new IllegalArgumentException("Animation state speed must be non-negative");
		}
	}

	private static void validate(String name, PAnimationType type, PInterpolationType interpolation)
	{
		require(name, "State name");
		Objects.requireNonNull(type);
		Objects.requireNonNull(interpolation);
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
