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

import java.util.List;

/**
 * Provides support for raw animation.
 */
public class PRawAnimation
{
	private final List<AnimationStage> stages;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param builder the builder to use.
	 */
	private PRawAnimation(Builder builder)
	{
		this.stages = List.copyOf(builder.stages);
	}
	
	/**
	 * Performs the begin operation.
	 * @return the value produced by this operation.
	 */
	public static Builder begin()
	{
		return new Builder();
	}
	
	/**
	 * Returns the stages.
	 * @return the value produced by this operation.
	 */
	public List<AnimationStage> getStages()
	{
		return this.stages;
	}
	
/**
 * Builds builder.
 */
	public static class Builder
	{
		private final List<AnimationStage> stages = new ObjectArrayList<>();
		
		/**
		 * Performs the then play operation.
		 * @param name the name to use.
		 * @return the value produced by this operation.
		 */
		public Builder thenPlay(String name)
		{
			return then(name, PAnimationType.PLAY_ONCE);
		}
		
		/**
		 * Performs the then loop operation.
		 * @param name the name to use.
		 * @return the value produced by this operation.
		 */
		public Builder thenLoop(String name)
		{
			return then(name, PAnimationType.CYCLE);
		}
		
		/**
		 * Performs the then hold operation.
		 * @param name the name to use.
		 * @return the value produced by this operation.
		 */
		public Builder thenHold(String name)
		{
			return then(name, PAnimationType.HOLD_LAST_FRAME);
		}
		
		/**
		 * Performs the then operation.
		 * @param name the name to use.
		 * @param type the type to use.
		 * @return the value produced by this operation.
		 */
		public Builder then(String name, PAnimationType type)
		{
			this.stages.add(new AnimationStage(
					name,
					type,
					PInterpolationType.LINEAR,
					1.0f,
					0
			));
			return this;
		}
		
		/**
		 * Performs the then wait operation.
		 * @param ticks the ticks to use.
		 * @return the value produced by this operation.
		 */
		public Builder thenWait(int ticks)
		{
			this.stages.add(new AnimationStage(
					AnimationStage.WAIT,
					PAnimationType.PLAY_ONCE,
					PInterpolationType.STEP,
					1.0f,
					ticks
			));
			return this;
		}
		
		/**
		 * Performs the with interpolation operation.
		 * @param interpolation the interpolation to use.
		 * @return the value produced by this operation.
		 */
		public Builder withInterpolation(PInterpolationType interpolation)
		{
			AnimationStage last = lastStage();
			
			replaceLast(new AnimationStage(
					last.animationName(),
					last.animationType(),
					interpolation,
					last.speed(),
					last.waitTicks()
			));
			
			return this;
		}
		
		/**
		 * Performs the with speed operation.
		 * @param speed the speed to use.
		 * @return the value produced by this operation.
		 */
		public Builder withSpeed(float speed)
		{
			if (!Float.isFinite(speed))
				throw new IllegalArgumentException("Animation stage speed must be finite");
			AnimationStage last = lastStage();
			
			replaceLast(new AnimationStage(
					last.animationName(),
					last.animationType(),
					last.interpolationType(),
					speed,
					last.waitTicks()
			));
			
			return this;
		}
		
		/**
		 * Performs the build operation.
		 * @return the value produced by this operation.
		 */
		public PRawAnimation build()
		{
			return new PRawAnimation(this);
		}
		
		/**
		 * Performs the last stage operation.
		 * @return the value produced by this operation.
		 */
		private AnimationStage lastStage()
		{
			if (this.stages.isEmpty())
				throw new IllegalStateException("No animation stages defined");
			
			return this.stages.getLast();
		}
		
		/**
		 * Replaces the last.
		 * @param stage the stage to use.
		 */
		private void replaceLast(AnimationStage stage)
		{
			this.stages.set(this.stages.size() - 1, stage);
		}
	}
	
/**
 * Immutable value object representing animation stage.
 */
	public record AnimationStage(
			String animationName,
			PAnimationType animationType,
			PInterpolationType interpolationType,
			float speed,
			int waitTicks
	)
	{
		public static final String WAIT = "pulse.internal.wait";
		
		/**
		 * Determines whether waiting.
		 * @return the value produced by this operation.
		 */
		public boolean isWaiting()
		{
			return WAIT.equals(this.animationName());
		}
	}
}
