/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;


import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Provides support for raw animation playback recipes.
 */
public class PRawAnimation
{
	private final List<PIAnimationStage> stages;

	/**
	 * Produces a deterministic pseudorandom value in the range {@code [0, 1)}.
	 * @param seed the input seed.
	 * @return a uniformly distributed pseudorandom value.
	 */
	public static double randomUnit(long seed)
	{
		long mixed = seed;
		mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
		mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
		mixed ^= mixed >>> 31;
		return (mixed >>> 11) * 0x1.0p-53;
	}

	private PRawAnimation(Builder builder)
	{
		this.stages = List.copyOf(builder.stages);
	}

	/**
	 * Begins an animation recipe.
	 * @return a new builder.
	 */
	public static Builder begin()
	{
		return new Builder();
	}

	/**
	 * Returns immutable stage definitions.
	 * @return the animation stages.
	 */
	public List<PIAnimationStage> getStages()
	{
		return this.stages;
	}

	/**
	 * Builds raw animation recipes.
	 */
	public static class Builder
	{
		private final List<PIAnimationStage> stages = new ObjectArrayList<>();

		/**
		 * Adds an arbitrary extensible animation stage.
		 * @param stage the stage to add.
		 * @return this builder.
		 */
		public Builder then(PIAnimationStage stage)
		{
			if (stage == null)
				throw new IllegalArgumentException("Animation stage must not be null");
			this.stages.add(stage);
			return this;
		}

		/**
		 * Adds a clip played once.
		 * @param name the animation name.
		 * @return this builder.
		 */
		public Builder thenPlay(String name)
		{
			return then(name, PAnimationType.PLAY_ONCE);
		}

		/**
		 * Adds a continuously cycled clip.
		 * @param name the animation name.
		 * @return this builder.
		 */
		public Builder thenLoop(String name)
		{
			return then(name, PAnimationType.CYCLE);
		}

		/**
		 * Adds a clip held on its final frame.
		 * @param name the animation name.
		 * @return this builder.
		 */
		public Builder thenHold(String name)
		{
			return then(name, PAnimationType.HOLD_LAST_FRAME);
		}

		/**
		 * Adds a clip with the supplied playback type.
		 * @param name the animation name.
		 * @param type the playback type.
		 * @return this builder.
		 */
		public Builder then(String name, PAnimationType type)
		{
			return then(new ClipStage(name, type, PInterpolationType.LINEAR, 1.0f));
		}

		/**
		 * Adds a wait stage.
		 * @param ticks the duration in ticks.
		 * @return this builder.
		 */
		public Builder thenWait(int ticks)
		{
			return then(new WaitStage(ticks));
		}

		/**
		 * Adds one weighted random clip, then advances to the next stage.
		 * @param configuration configures the random pool.
		 * @return this builder.
		 */
		public Builder thenRandom(Consumer<RandomStageBuilder> configuration)
		{
			return thenRandom(configuration, false);
		}

		/**
		 * Adds a weighted random pool that selects a new clip after each completion.
		 * @param configuration configures the random pool.
		 * @return this builder.
		 */
		public Builder thenRandomLoop(Consumer<RandomStageBuilder> configuration)
		{
			return thenRandom(configuration, true);
		}

		private Builder thenRandom(Consumer<RandomStageBuilder> configuration, boolean loop)
		{
			if (configuration == null)
				throw new IllegalArgumentException("Random stage configuration must not be null");
			RandomStageBuilder random = new RandomStageBuilder();
			configuration.accept(random);
			return then(random.build(loop));
		}

		/**
		 * Changes the interpolation of the most recently added stage.
		 * @param interpolation the interpolation to use.
		 * @return this builder.
		 */
		public Builder withInterpolation(PInterpolationType interpolation)
		{
			replaceLast(lastStage().withInterpolation(interpolation));
			return this;
		}

		/**
		 * Changes the speed of the most recently added stage.
		 * @param speed the speed to use.
		 * @return this builder.
		 */
		public Builder withSpeed(float speed)
		{
			replaceLast(lastStage().withSpeed(speed));
			return this;
		}

		/**
		 * Builds the animation recipe.
		 * @return the raw animation.
		 */
		public PRawAnimation build()
		{
			return new PRawAnimation(this);
		}

		private PIAnimationStage lastStage()
		{
			if (this.stages.isEmpty())
				throw new IllegalStateException("No animation stages defined");
			return this.stages.getLast();
		}

		private void replaceLast(PIAnimationStage stage)
		{
			this.stages.set(this.stages.size() - 1, stage);
		}
	}

	/**
	 * Configures a weighted random animation pool.
	 */
	public static class RandomStageBuilder
	{
		private final List<RandomEntry> entries = new ObjectArrayList<>();
		private boolean preventImmediateRepeat;

		/**
		 * Adds an animation with its relative weight.
		 * @param animationName the animation name.
		 * @param weight the positive relative weight.
		 * @return this builder.
		 */
		public RandomStageBuilder add(String animationName, float weight)
		{
			this.entries.add(new RandomEntry(animationName, weight));
			return this;
		}

		/**
		 * Prevents selecting the same animation twice in succession where possible.
		 * @return this builder.
		 */
		public RandomStageBuilder preventImmediateRepeat()
		{
			this.preventImmediateRepeat = true;
			return this;
		}

		private RandomStage build(boolean loop)
		{
			return new RandomStage(this.entries, loop, this.preventImmediateRepeat,
					PInterpolationType.LINEAR, 1.0f);
		}
	}

	private record ClipStage(
			String animationName,
			PAnimationType animationType,
			PInterpolationType interpolationType,
			float speed
	) implements PIAnimationStage
	{
		private ClipStage
		{
			PAnimationStagePlayback.clip(animationName, animationType, interpolationType, speed);
		}

		@Override
		public PAnimationStagePlayer createPlayer(PAnimationStageContext context)
		{
			return new StaticStagePlayer(PAnimationStagePlayback.clip(
					this.animationName, this.animationType, this.interpolationType, this.speed));
		}

		@Override
		public PIAnimationStage withInterpolation(PInterpolationType interpolation)
		{
			return new ClipStage(this.animationName, this.animationType, interpolation, this.speed);
		}

		@Override
		public PIAnimationStage withSpeed(float speed)
		{
			return new ClipStage(this.animationName, this.animationType, this.interpolationType, speed);
		}
	}

	private record WaitStage(int ticks) implements PIAnimationStage
	{
		private WaitStage
		{
			PAnimationStagePlayback.waitFor(ticks);
		}

		@Override
		public PAnimationStagePlayer createPlayer(PAnimationStageContext context)
		{
			return new StaticStagePlayer(PAnimationStagePlayback.waitFor(this.ticks));
		}

		@Override
		public PIAnimationStage withInterpolation(PInterpolationType interpolation)
		{
			if (interpolation == null)
				throw new IllegalArgumentException("Interpolation type must not be null");
			return this;
		}

		@Override
		public PIAnimationStage withSpeed(float speed)
		{
			if (!Float.isFinite(speed))
				throw new IllegalArgumentException("Animation speed must be finite");
			return this;
		}
	}

	private record RandomStage(
			List<RandomEntry> entries,
			boolean loop,
			boolean preventImmediateRepeat,
			PInterpolationType interpolationType,
			float speed
	) implements PIAnimationStage
	{
		private RandomStage
		{
			if (entries == null || entries.isEmpty())
				throw new IllegalArgumentException("Random animation stage needs at least one entry");
			entries = List.copyOf(entries);
			if (interpolationType == null)
				throw new IllegalArgumentException("Interpolation type must not be null");
			if (!Float.isFinite(speed))
				throw new IllegalArgumentException("Animation speed must be finite");

			Set<String> names = new HashSet<>();
			double totalWeight = 0.0d;
			for (RandomEntry entry : entries)
			{
				if (!names.add(entry.animationName()))
					throw new IllegalArgumentException("Random animation stage must not contain duplicate animation names");
				totalWeight += entry.weight();
			}
			if (!Double.isFinite(totalWeight) || totalWeight <= 0.0d)
				throw new IllegalArgumentException("Total animation weight must be positive and finite");
		}

		@Override
		public PAnimationStagePlayer createPlayer(PAnimationStageContext context)
		{
			return new RandomStagePlayer(this, context.randomSeed());
		}

		@Override
		public PIAnimationStage withInterpolation(PInterpolationType interpolation)
		{
			return new RandomStage(this.entries, this.loop, this.preventImmediateRepeat, interpolation, this.speed);
		}

		@Override
		public PIAnimationStage withSpeed(float speed)
		{
			return new RandomStage(this.entries, this.loop, this.preventImmediateRepeat, this.interpolationType, speed);
		}
	}

	private record RandomEntry(String animationName, float weight)
	{
		private RandomEntry
		{
			if (animationName == null || animationName.isBlank())
				throw new IllegalArgumentException("Animation name must not be blank");
			if (!Float.isFinite(weight) || weight <= 0.0f)
				throw new IllegalArgumentException("Animation weight must be positive and finite");
		}
	}

	private record StaticStagePlayer(PAnimationStagePlayback playback) implements PAnimationStagePlayer
	{
		@Override
		public PAnimationStagePlayback currentPlayback()
		{
			return this.playback;
		}

		@Override
		public PAnimationStageTransition complete()
		{
			return PAnimationStageTransition.NEXT_STAGE;
		}
	}

	private static class RandomStagePlayer implements PAnimationStagePlayer
	{
		private final RandomStage stage;
		private final long randomSeed;
		private long selectionIndex;
		private String currentName;
		private String previousName;

		private RandomStagePlayer(RandomStage stage, long randomSeed)
		{
			this.stage = stage;
			this.randomSeed = randomSeed;
			selectNext();
		}

		@Override
		public PAnimationStagePlayback currentPlayback()
		{
			return PAnimationStagePlayback.clip(this.currentName, PAnimationType.PLAY_ONCE,
					this.stage.interpolationType(), this.stage.speed());
		}

		@Override
		public PAnimationStageTransition complete()
		{
			if (!this.stage.loop())
				return PAnimationStageTransition.NEXT_STAGE;
			selectNext();
			return PAnimationStageTransition.REPEAT_PLAYBACK;
		}

		private void selectNext()
		{
			String excluded = this.stage.preventImmediateRepeat() && this.stage.entries().size() > 1 ?
					this.previousName : null;
			double totalWeight = 0.0d;
			for (RandomEntry entry : this.stage.entries())
				if (!entry.animationName().equals(excluded))
					totalWeight += entry.weight();

			double value = nextRandomDouble() * totalWeight;
		RandomEntry fallback = null;
		for (RandomEntry entry : this.stage.entries())
			{
				if (entry.animationName().equals(excluded))
					continue;
				fallback = entry;
				value -= entry.weight();
				if (value < 0.0d)
				{
					this.currentName = entry.animationName();
					this.previousName = this.currentName;
					return;
				}
			}

			if (fallback == null)
				throw new IllegalStateException("Random animation stage has no selectable entries");
			this.currentName = fallback.animationName();
			this.previousName = this.currentName;
		}

		private double nextRandomDouble()
		{
			long seed = this.randomSeed + this.selectionIndex++ * 0x9E3779B97F4A7C15L;
			return randomUnit(seed);
		}
	}
}
