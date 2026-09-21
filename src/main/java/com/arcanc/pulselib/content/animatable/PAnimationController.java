/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;


import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationGraph;
import com.arcanc.pulselib.content.model.animation.PAnimationGraphRuntime;
import com.arcanc.pulselib.content.model.animation.PCompiledAnimation;
import com.arcanc.pulselib.content.model.animation.PAnimationType;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Provides support for animation controller.
 */
public class PAnimationController<T extends PAnimatable<T>>
{
	protected final String name;
	protected final StateHandler<T> stateHandler;
	
	protected @Nullable PRawAnimation currentAnimation;
	
	private int stageIndex;
	private float time;
	private float prevTime;
	private boolean stageStarted = true;
	private ControllerState state;
	private final MolangParser.Context persistentMolangContext = new MolangParser.Context();
	private @Nullable PAnimationGraphRuntime graphRuntime;
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param stateHandler the state handler to use.
	 */
	public PAnimationController(StateHandler<T> stateHandler)
	{
		this("default", stateHandler);
	}
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param name the name to use.
	 * @param stateHandler the state handler to use.
	 */
	public PAnimationController(String name, StateHandler<T> stateHandler)
	{
		this.name = name;
		this.stateHandler = stateHandler;
		this.state = ControllerState.STOP;
	}
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param graph the graph to use.
	 */
	public PAnimationController(PAnimationGraph graph)
	{
		this("default", graph);
	}
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param name the name to use.
	 * @param graph the graph to use.
	 */
	public PAnimationController(String name, PAnimationGraph graph)
	{
		this(name, state -> ControllerState.PLAY);
		play(graph);
	}
	
	/**
	 * Performs the play operation.
	 * @param graph the graph to use.
	 */
	public void play(PAnimationGraph graph)
	{
		this.currentAnimation = null;
		this.stageIndex = 0;
		this.time = 0.0f;
		this.prevTime = 0.0f;
		this.stageStarted = true;
		this.graphRuntime = new PAnimationGraphRuntime(graph);
		this.state = ControllerState.PLAY;
	}

	/**
	 * Performs the graph runtime operation.
	 * @return the value produced by this operation.
	 */
	public @Nullable PAnimationGraphRuntime graphRuntime()
	{
		return this.graphRuntime;
	}

	/**
	 * Sets the parameter.
	 * @param name the name to use.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	public PAnimationController<T> setParameter(String name, float value)
	{
		if (this.graphRuntime == null)
			throw new IllegalStateException("This controller does not have an animation graph");
		this.graphRuntime.parameters().set(name, value);
		return this;
	}

	/**
	 * Sets the parameter.
	 * @param name the name to use.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	public PAnimationController<T> setParameter(String name, boolean value)
	{
		if (this.graphRuntime == null)
			throw new IllegalStateException("This controller does not have an animation graph");
		this.graphRuntime.parameters().set(name, value);
		return this;
	}

	/**
	 * Performs the trigger operation.
	 * @param name the name to use.
	 * @return the value produced by this operation.
	 */
	public PAnimationController<T> trigger(String name)
	{
		if (this.graphRuntime == null)
			throw new IllegalStateException("This controller does not have an animation graph");
		this.graphRuntime.parameters().trigger(name);
		return this;
	}
	
	/**
	 * Performs the play operation.
	 * @param animation the animation to use.
	 */
	public void play(PRawAnimation animation)
	{
		this.graphRuntime = null;
		if (this.currentAnimation == animation)
		{
			PRawAnimation.AnimationStage stage = getCurrentStage();
			if (stage != null && stage.animationType() == PAnimationType.HOLD_LAST_FRAME)
				if (this.state == ControllerState.PAUSE)
					return;
			if (this.state == ControllerState.PLAY)
				return;
		}
		
		this.currentAnimation = animation;
		this.stageIndex = 0;
		this.time = 0;
		this.prevTime = 0;
		this.stageStarted = true;
		this.state = ControllerState.PLAY;
	}
	
	/**
	 * Performs the pause operation.
	 */
	public void pause()
	{
		if (this.state == ControllerState.PLAY)
			this.state = ControllerState.PAUSE;
	}
	
	/**
	 * Performs the resume operation.
	 */
	public void resume()
	{
		if (this.state == ControllerState.PAUSE)
			this.state = ControllerState.PLAY;
	}
	
	/**
	 * Performs the stop operation.
	 */
	public void stop()
	{
		this.currentAnimation = null;
		this.stageIndex = 0;
		this.prevTime = 0;
		this.time = 0;
		this.stageStarted = true;
		this.state = ControllerState.STOP;
	}
	
	/**
	 * Returns the state.
	 * @return the value produced by this operation.
	 */
	public ControllerState getState()
	{
		return this.state;
	}
	
	/**
	 * Determines whether playing.
	 * @return the value produced by this operation.
	 */
	public boolean isPlaying()
	{
		return this.state == ControllerState.PLAY;
	}
	
	/**
	 * Determines whether paused.
	 * @return the value produced by this operation.
	 */
	public boolean isPaused()
	{
		return this.state == ControllerState.PAUSE;
	}
	
	/**
	 * Determines whether stopped.
	 * @return the value produced by this operation.
	 */
	public boolean isStopped()
	{
		return this.state == ControllerState.STOP;
	}
	
	/**
	 * Calculates the bone transformations.
	 * @param boneName the bone name to use.
	 * @param model the model to use.
	 * @param partialTick the partial tick to use.
	 * @param molangContext the molang context to use.
	 * @param accumulatedFrame the accumulated frame to use.
	 * @return the value produced by this operation.
	 */
	public @Nullable BoneFrame calculateBoneTransformations(String boneName,
	                                                        PBakedModel model,
	                                                        float partialTick,
	                                                        MolangParser.Context molangContext,
	                                                        @Nullable BoneFrame accumulatedFrame)
	{
		if (this.state == ControllerState.STOP)
			return null;
		
		PRawAnimation.AnimationStage stage = getCurrentStage();
		if (stage == null)
			return null;
		
		PAnimation animation = model.animations().get(stage.animationName());
		
		if (animation == null)
			return null;
		
		float animationTime = this.getInterpolatedTime(partialTick);
		return animation.calculateBoneTransformations(
				boneName,
				animationTime,
				stage.interpolationType(),
				this.persistentMolangContext.copyFrameValuesFrom(molangContext),
				accumulatedFrame);
	}

	/**
	 * Calculates the bone transformations.
	 * @param animation the animation to use.
	 * @param boneIndex the bone index to use.
	 * @param partialTick the partial tick to use.
	 * @param molangContext the molang context to use.
	 * @param accumulatedFrame the accumulated frame to use.
	 * @return the value produced by this operation.
	 */
	public @Nullable BoneFrame calculateBoneTransformations(PCompiledAnimation animation,
	                                                        int boneIndex,
	                                                        float partialTick,
	                                                        MolangParser.Context molangContext,
	                                                        @Nullable BoneFrame accumulatedFrame)
	{
		if (this.state == ControllerState.STOP)
			return null;
		PRawAnimation.AnimationStage stage = getCurrentStage();
		if (stage == null || stage.isWaiting() || animation.boneAnimation(boneIndex) == null)
			return null;
		return animation.animation().calculateBoneTransformations(
				animation.boneAnimation(boneIndex),
				this.getInterpolatedTime(partialTick),
				stage.interpolationType(),
				this.persistentMolangContext.copyFrameValuesFrom(molangContext),
				accumulatedFrame);
	}

	/**
	 * Performs the persistent molang context operation.
	 * @return the value produced by this operation.
	 */
	public MolangParser.Context persistentMolangContext()
	{
		return this.persistentMolangContext;
	}

	/**
	 * Performs the tick operation.
	 * @param animatable the animatable to use.
	 * @param tickCount the tick count to use.
	 * @param model the model to use.
	 */
	public void tick(T animatable, float tickCount, PBakedModel model)
	{
		tick(animatable, tickCount, model, List.of(this));
	}
	
	/**
	 * Performs the tick operation.
	 * @param animatable the animatable to use.
	 * @param tickCount the tick count to use.
	 * @param model the model to use.
	 * @param poseControllers the pose controllers to use.
	 */
	public void tick(T animatable, float tickCount, PBakedModel model, Collection<PAnimationController<T>> poseControllers)
	{
		if (!Float.isFinite(tickCount))
			throw new IllegalArgumentException("Animation tick delta must be finite");
		if (this.graphRuntime != null)
		{
			if (this.state == ControllerState.PLAY)
				for (PAnimationGraphRuntime.EventTrack track : this.graphRuntime.tick(model, tickCount))
				{
					PAnimation animation = model == null ? null : model.animations().get(track.animation());
					if (animation != null)
						fireEvents(animatable, model, poseControllers, animation, track.from(), track.to(), track.animationType());
				}
			return;
		}
		ControllerState newState = this.stateHandler.handle(new AnimatableState<>(animatable, this));
		if (newState != this.state)
			this.state = newState;
		
		if (this.state != ControllerState.PLAY)
			return;
		
		if (this.currentAnimation == null)
		{
			this.state = ControllerState.STOP;
			return;
		}
		
		if (this.stageIndex >= this.currentAnimation.getStages().size())
		{
			this.state = ControllerState.STOP;
			return;
		}
		
		if (model == null)
			return;
		
		PRawAnimation.AnimationStage stage = this.currentAnimation.getStages().get(this.stageIndex);
		
		if (stage.isWaiting())
		{
			this.prevTime = this.time;
			this.time += tickCount;
			if (this.time >= stage.waitTicks())
				nextStage();
			return;
		}
		
		PAnimation animation = model.animations().get(stage.animationName());
		if (animation == null)
		{
			nextStage();
			return;
		}
		
		float length = animation.length();
		if (this.stageStarted)
		{
			this.time = stage.speed() < 0.0f ? length : 0.0f;
			this.prevTime = this.time;
			this.stageStarted = false;
		}
		this.prevTime = this.time;
		float nextTime = this.time + tickCount * stage.speed();
		switch (stage.animationType())
		{
			case PLAY_ONCE ->
			{
				this.time = nextTime;
				float bounded = Math.clamp(this.time, 0.0f, length);
				fireEvents(animatable, model, poseControllers, animation, this.prevTime, bounded, PAnimationType.PLAY_ONCE);
				if ((stage.speed() >= 0.0f && this.time >= length) || (stage.speed() < 0.0f && this.time <= 0.0f))
					nextStage();
			}
			case HOLD_LAST_FRAME ->
			{
				this.time = nextTime;
				float bounded = Math.clamp(this.time, 0.0f, length);
				fireEvents(animatable, model, poseControllers, animation, this.prevTime, bounded, PAnimationType.HOLD_LAST_FRAME);
				if ((stage.speed() >= 0.0f && this.time >= length) || (stage.speed() < 0.0f && this.time <= 0.0f))
				{
					this.time = stage.speed() < 0.0f ? 0.0f : length;
					this.state = ControllerState.PAUSE;
				}
			}
			case CYCLE ->
			{
				if (length > 0)
				{
					fireEvents(animatable, model, poseControllers, animation, this.prevTime, nextTime, PAnimationType.CYCLE);
					this.time = floorMod(nextTime, length);
				}
				else
					this.time = nextTime;
			}
		}
	}
	
	/**
	 * Fires the events.
	 * @param animatable the animatable to use.
	 * @param model the model to use.
	 * @param poseControllers the pose controllers to use.
	 * @param animation the animation to use.
	 * @param from the from to use.
	 * @param to the to to use.
	 * @param type the type to use.
	 */
	private void fireEvents(T animatable,
	                        PBakedModel model,
	                        Collection<PAnimationController<T>> poseControllers,
	                        PAnimation animation,
	                        float from,
	                        float to,
	                        PAnimationType type)
	{
		if (type == PAnimationType.CYCLE && animation.length() > 0.0f)
			fireCyclicEvents(animatable, model, poseControllers, animation, from, to);
		else if (to >= from)
			animation.eventsBetween(from, to).forEach(event -> PAnimationEventDispatcher.dispatch(animatable, this, event, model, poseControllers));
		else
			animation.eventsBetweenReverse(from, to).forEach(event -> PAnimationEventDispatcher.dispatch(animatable, this, event, model, poseControllers));
	}

	/**
	 * Fires the cyclic events.
	 * @param animatable the animatable to use.
	 * @param model the model to use.
	 * @param controllers the controllers to use.
	 * @param animation the animation to use.
	 * @param from the from to use.
	 * @param to the to to use.
	 */
	private void fireCyclicEvents(T animatable, PBakedModel model, Collection<PAnimationController<T>> controllers,
	                              PAnimation animation, float from, float to)
	{
		float length = animation.length();
		if (to >= from)
		{
			for (float cursor = from; cursor < to; )
			{
				float boundary = ((float)Math.floor(cursor / length) + 1.0f) * length;
				float end = Math.min(to, boundary);
				float localFrom = floorMod(cursor, length);
				float localTo = end == boundary ? length : floorMod(end, length);
				animation.eventsBetween(localFrom, localTo).forEach(event -> PAnimationEventDispatcher.dispatch(animatable, this, event, model, controllers));
				if (end >= to) break;
				cursor = end;
			}
		}
		else
		{
			boolean wrapped = false;
			for (float cursor = from; cursor > to; )
			{
				float boundary = ((float)Math.ceil(cursor / length) - 1.0f) * length;
				float end = Math.max(to, boundary);
				float localFrom = floorMod(cursor, length);
				if (localFrom == 0.0f && (cursor > 0.0f || wrapped)) localFrom = length;
				float localTo = end == boundary ? 0.0f : floorMod(end, length);
				animation.eventsBetweenReverse(localFrom, localTo).forEach(event -> PAnimationEventDispatcher.dispatch(animatable, this, event, model, controllers));
				if (end <= to) break;
				// Crossing zero wraps onto the end marker of the previous cycle.
				animation.eventsBetween(length - 1.0e-6f, length).forEach(event ->
						PAnimationEventDispatcher.dispatch(animatable, this, event, model, controllers));
				wrapped = true;
				cursor = end;
			}
		}
	}

	/**
	 * Performs the floor mod operation.
	 * @param value the value to use.
	 * @param modulus the modulus to use.
	 * @return the value produced by this operation.
	 */
	private static float floorMod(float value, float modulus)
	{
		float result = value % modulus;
		return result < 0.0f ? result + modulus : result;
	}
	
	/**
	 * Performs the next stage operation.
	 */
	private void nextStage()
	{
		this.stageIndex++;
		this.time = 0;
		this.prevTime = 0;
		this.stageStarted = true;
		
		if (this.currentAnimation == null)
		{
			this.state = ControllerState.STOP;
			return;
		}
		
		if (this.stageIndex >= this.currentAnimation.getStages().size())
			this.state = ControllerState.STOP;
	}
	
	/**
	 * Returns the interpolated time.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public float getInterpolatedTime(float partialTick)
	{
		if (this.graphRuntime != null)
			return this.graphRuntime.interpolatedTime(partialTick);
		if  (this.time < this.prevTime)
			return Mth.lerp(partialTick, this.prevTime, this.prevTime + this.time);
		return Mth.lerp(partialTick, this.prevTime, this.time);
	}
	
	/**
	 * Returns the time.
	 * @return the value produced by this operation.
	 */
	public float getTime()
	{
		if (this.graphRuntime != null)
			return this.graphRuntime.time();
		return this.time;
	}
	
	/**
	 * Performs the cycle phase operation.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	public float cyclePhase(PBakedModel model)
	{
		if (this.graphRuntime != null)
			return this.graphRuntime.cyclePhase(model);
		PRawAnimation.AnimationStage stage = getCurrentStage();
		if (stage == null || stage.animationType() != PAnimationType.CYCLE)
			return Float.NaN;
		PAnimation animation = model.animations().get(stage.animationName());
		return animation == null || animation.length() <= 0.0f ? Float.NaN : this.time / animation.length();
	}
	
	/**
	 * Synchronizes the cycle.
	 * @param model the model to use.
	 * @param phase the phase to use.
	 */
	public void syncCycle(PBakedModel model, float phase)
	{
		if (this.graphRuntime != null)
		{
			this.graphRuntime.syncCycle(model, phase);
			return;
		}
		PRawAnimation.AnimationStage stage = getCurrentStage();
		if (stage == null || stage.animationType() != PAnimationType.CYCLE)
			return;
		PAnimation animation = model.animations().get(stage.animationName());
		if (animation == null || animation.length() <= 0.0f)
			return;
		this.time = Math.clamp(phase, 0.0f, 1.0f) * animation.length();
		this.prevTime = this.time;
		this.stageStarted = false;
	}

	public void seek(PBakedModel model, float animationTime)
	{
		if (this.graphRuntime != null)
			return;
		PRawAnimation.AnimationStage stage = getCurrentStage();
		if (stage == null || stage.isWaiting())
			return;
		PAnimation animation = model.animations().get(stage.animationName());
		if (animation == null || animation.length() <= 0.0f)
			return;
		this.time = stage.animationType() == PAnimationType.CYCLE ? floorMod(animationTime, animation.length()) :
				Math.clamp(animationTime, 0.0f, animation.length());
		this.prevTime = this.time;
		this.stageStarted = false;
	}
	
	/**
	 * Returns the current stage.
	 * @return the value produced by this operation.
	 */
	public PRawAnimation.@Nullable AnimationStage getCurrentStage()
	{
		if (this.currentAnimation == null)
			return null;
		if (this.stageIndex >= this.currentAnimation.getStages().size())
			return null;
		return this.currentAnimation.getStages().get(this.stageIndex);
	}

	/**
	 * Performs the graph layers operation.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	public List<PAnimationGraphRuntime.Layer> graphLayers(PBakedModel model)
	{
		if (this.graphRuntime == null || this.state == ControllerState.STOP)
			return List.of();
		return this.graphRuntime.layers(model);
	}
	
	@FunctionalInterface
/**
 * Defines the contract for state handler.
 */
	public interface StateHandler<T extends PAnimatable<T>>
	{
		/**
		 * Performs the handle operation.
		 * @param state the state to use.
		 * @return the value produced by this operation.
		 */
		ControllerState handle(AnimatableState<T> state);
	}

	/**
	 * Performs the name operation.
	 * @return the value produced by this operation.
	 */
	public String name()
	{
		return this.name;
	}
	
/**
 * Immutable value object representing animatable state.
 */
	public record AnimatableState<T extends PAnimatable<T>>(T animatable, PAnimationController<T> controller)
	{
	}
}
