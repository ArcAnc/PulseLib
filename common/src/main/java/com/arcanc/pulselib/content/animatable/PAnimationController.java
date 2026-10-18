/**
 * @author ArcAnc
 * Created at: 24.02.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;


import com.arcanc.pulselib.content.model.animation.*;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.data.gecko.MolangParser;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Owns raw-animation or graph playback for one animatable controller. Playback positions are measured in
 * game ticks; the persistent Molang context keeps instance variables across evaluations.
 *
 * @param <T> the animatable owner type
 */
public class PAnimationController<T extends PAnimatable<T>>
{
	/**
	 * Returns the registration name of this controller.
	 */
	protected final String name;
	/**
	 * Callback that requests the controller state on each owner tick.
	 */
	protected final StateHandler<T> stateHandler;
	
	/**
	 * Selected raw animation sequence, or {@code null} when no raw sequence is active.
	 */
	protected @Nullable PRawAnimation currentAnimation;
	
	private int stageIndex;
	private @Nullable PAnimationStagePlayer stagePlayer;
	private @Nullable PAnimationStagePlayback currentPlayback;
	private float time;
	private float prevTime;
	private float interpolationFrom;
	private float interpolationTo;
	private float interpolationCycleLength;
	private boolean stageStarted = true;
	private ControllerState state;
	private final long randomSeed;
	private final MolangParser.Context persistentMolangContext = new MolangParser.Context();
	private @Nullable PAnimationGraphRuntime graphRuntime;
	private @Nullable PAnimationPoseResolver.MolangContextProvider<T> graphSnapshotContexts;
	
	/**
	 * Creates a controller with the supplied state handler or graph and controller identity.
	 *
	 * @param stateHandler the callback selecting the controller playback state
	 */
	public PAnimationController(StateHandler<T> stateHandler)
	{
		this("default", stateHandler);
	}
	
	/**
	 * Creates a controller with the supplied state handler or graph and controller identity.
	 *
	 * @param name the registered controller name
	 * @param stateHandler the callback selecting the controller playback state
	 */
	public PAnimationController(String name, StateHandler<T> stateHandler)
	{
		this(name, stateHandler, name.hashCode());
	}

	/**
	 * Creates a controller with the supplied state handler or graph and controller identity.
	 *
	 * @param name the registered controller name
	 * @param stateHandler the callback selecting the controller playback state
	 * @param randomSeed the deterministic seed used by runtime animation stages
	 */
	public PAnimationController(String name, StateHandler<T> stateHandler, long randomSeed)
	{
		this.name = name;
		this.stateHandler = stateHandler;
		this.randomSeed = randomSeed;
		this.state = ControllerState.STOP;
	}
	
	/**
	 * Creates a controller with the supplied state handler or graph and controller identity.
	 *
	 * @param graph the immutable animation state graph
	 */
	public PAnimationController(PAnimationGraph graph)
	{
		this("default", graph);
	}
	
	/**
	 * Creates a controller with the supplied state handler or graph and controller identity.
	 *
	 * @param name the registered controller name
	 * @param graph the immutable animation state graph
	 */
	public PAnimationController(String name, PAnimationGraph graph)
	{
		this(name, state -> ControllerState.PLAY);
		play(graph);
	}
	
	/**
	 * Switches to the supplied animation graph and resets playback, transitions and interpolation history.
	 *
	 * @param graph the immutable animation state graph
	 */
	public void play(PAnimationGraph graph)
	{
		this.currentAnimation = null;
		this.stageIndex = 0;
		this.stagePlayer = null;
		this.currentPlayback = null;
		this.time = 0.0f;
		this.prevTime = 0.0f;
		this.interpolationFrom = 0.0f;
		this.interpolationTo = 0.0f;
		this.interpolationCycleLength = 0.0f;
		this.stageStarted = true;
		this.graphRuntime = new PAnimationGraphRuntime(graph);
		this.state = ControllerState.PLAY;
	}

	/**
	 * Returns the active graph runtime, or {@code null} while this controller uses raw animation playback.
	 *
	 * @return the active graph runtime, or {@code null} while this controller uses raw animation playback
	 */
	public @Nullable PAnimationGraphRuntime graphRuntime()
	{
		return this.graphRuntime;
	}

	/**
	 * Supplies current game queries for RESTART sampling at the tick boundary (partialTick = 1).
	 * Use the same query population as the renderer. The provider also resolves preceding controllers;
	 * persistent variables remain owned by each controller. Existing tick signatures are unchanged.
	 * @param contexts the provider of fresh frame values at capture time.
	 * @return this controller.
	 */
	public PAnimationController<T> setGraphSnapshotContexts(PAnimationPoseResolver.MolangContextProvider<T> contexts)
	{
		this.graphSnapshotContexts = Objects.requireNonNull(contexts);
		return this;
	}

	/**
	 * Updates a named parameter of the active animation graph.
	 *
	 * @param name the graph parameter name
	 * @param value the value written by this operation
	 * @return this instance
	 */
	public PAnimationController<T> setParameter(String name, float value)
	{
		if (this.graphRuntime == null)
			throw new IllegalStateException("This controller does not have an animation graph");
		this.graphRuntime.parameters().set(name, value);
		return this;
	}

	/**
	 * Updates a named parameter of the active animation graph.
	 *
	 * @param name the graph parameter name
	 * @param value the value written by this operation
	 * @return this instance
	 */
	public PAnimationController<T> setParameter(String name, boolean value)
	{
		if (this.graphRuntime == null)
			throw new IllegalStateException("This controller does not have an animation graph");
		this.graphRuntime.parameters().set(name, value);
		return this;
	}

	/**
	 * Raises a named trigger in the active animation graph.
	 *
	 * @param name the graph parameter name
	 * @return this instance
	 */
	public PAnimationController<T> trigger(String name)
	{
		if (this.graphRuntime == null)
			throw new IllegalStateException("This controller does not have an animation graph");
		this.graphRuntime.parameters().trigger(name);
		return this;
	}
	
	/**
	 * Selects a raw animation sequence, retaining an already active identical sequence when its playback state
	 * allows it.
	 *
	 * @param animation the raw animation sequence to play
	 */
	public void play(PRawAnimation animation)
	{
		this.graphRuntime = null;
		if (this.currentAnimation == animation)
		{
			PAnimationStagePlayback stage = getCurrentStage();
			if (stage != null && stage.animationType() == PAnimationType.HOLD_LAST_FRAME)
				if (this.state == ControllerState.PAUSE)
					return;
			if (this.state == ControllerState.PLAY)
				return;
		}
		
		this.currentAnimation = animation;
		this.stageIndex = 0;
		this.state = ControllerState.PLAY;
		enterCurrentStage();
	}
	
	/**
	 * Pauses an actively playing controller without clearing its playback position.
	 */
	public void pause()
	{
		if (this.state == ControllerState.PLAY)
			this.state = ControllerState.PAUSE;
	}
	
	/**
	 * Resumes a paused controller from its current playback position.
	 */
	public void resume()
	{
		if (this.state == ControllerState.PAUSE)
			this.state = ControllerState.PLAY;
	}
	
	/**
	 * Clears raw playback state and resets the controller clock to zero.
	 */
	public void stop()
	{
		this.currentAnimation = null;
		this.stageIndex = 0;
		this.stagePlayer = null;
		this.currentPlayback = null;
		this.prevTime = 0;
		this.time = 0;
		this.interpolationFrom = 0.0f;
		this.interpolationTo = 0.0f;
		this.interpolationCycleLength = 0.0f;
		this.stageStarted = true;
		this.state = ControllerState.STOP;
	}
	
	/**
	 * Returns the current controller playback state.
	 *
	 * @return the current controller playback state
	 */
	public ControllerState getState()
	{
		return this.state;
	}
	
	/**
	 * Returns whether the controller is advancing playback.
	 *
	 * @return whether the controller is advancing playback
	 */
	public boolean isPlaying()
	{
		return this.state == ControllerState.PLAY;
	}
	
	/**
	 * Returns whether playback is paused at its current position.
	 *
	 * @return whether playback is paused at its current position
	 */
	public boolean isPaused()
	{
		return this.state == ControllerState.PAUSE;
	}
	
	/**
	 * Returns whether the controller is stopped.
	 *
	 * @return whether the controller is stopped
	 */
	public boolean isStopped()
	{
		return this.state == ControllerState.STOP;
	}
	
	/**
	 * Samples this controller's transform contribution for one bone at the interpolated playback position.
	 *
	 * @param boneName the animation bone name
	 * @param model the baked model supplying indexed bones and animations
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param molangContext the Molang values used for this animation sample
	 * @param accumulatedFrame the transform contributed by preceding controllers; may be {@code null}
	 * @return the sampled transform contribution for the selected bone, or {@code null} when playback or the selected bone track has no contribution
	 */
	public @Nullable BoneFrame calculateBoneTransformations(String boneName,
	                                                        PBakedModel model,
	                                                        float partialTick,
	                                                        MolangParser.Context molangContext,
	                                                        @Nullable BoneFrame accumulatedFrame)
	{
		if (this.state == ControllerState.STOP)
			return null;
		
		PAnimationStagePlayback stage = getCurrentStage();
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
	 * Samples this controller's transform contribution for one bone at the interpolated playback position.
	 *
	 * @param animation the animation
	 * @param boneIndex the zero-based baked-model bone index
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param molangContext the Molang values used for this animation sample
	 * @param accumulatedFrame the transform contributed by preceding controllers; may be {@code null}
	 * @return the sampled transform contribution for the selected bone, or {@code null} when playback or the selected bone track has no contribution
	 */
	public @Nullable BoneFrame calculateBoneTransformations(PCompiledAnimation animation,
	                                                        int boneIndex,
	                                                        float partialTick,
	                                                        MolangParser.Context molangContext,
	                                                        @Nullable BoneFrame accumulatedFrame)
	{
		if (this.state == ControllerState.STOP)
			return null;
		PAnimationStagePlayback stage = getCurrentStage();
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
	 * Returns the controller-owned Molang context whose instance variables persist between samples.
	 *
	 * @return the controller-owned Molang context whose instance variables persist between samples
	 */
	public MolangParser.Context persistentMolangContext()
	{
		return this.persistentMolangContext;
	}

	/**
	 * Updates the requested controller state and advances playback using the owner tick count and bound model.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param tickCount the owner tick count used to advance controller playback
	 * @param model the baked model supplying indexed bones and animations
	 */
	public void tick(T animatable, float tickCount, PBakedModel model)
	{
		tick(animatable, tickCount, model, List.of(this));
	}
	
	/**
	 * Updates the requested controller state and advances playback using the owner tick count and bound model.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @param tickCount the owner tick count used to advance controller playback
	 * @param model the baked model supplying indexed bones and animations
	 * @param poseControllers the controllers contributing to locator or pose resolution
	 */
	public void tick(T animatable, float tickCount, PBakedModel model, Collection<PAnimationController<T>> poseControllers)
	{
		if (!Float.isFinite(tickCount))
			throw new IllegalArgumentException("Animation tick delta must be finite");
		if (this.graphRuntime != null)
		{
			if (this.state == ControllerState.PLAY)
				for (PAnimationGraphRuntime.PreciseEventTrack track : this.graphRuntime.tickPrecise(model, tickCount,
						(snapshotModel, runtime) -> this.graphSnapshotContexts == null ?
								PAnimationRuntime.captureGraphBase(snapshotModel, this, poseControllers) :
								PAnimationRuntime.captureGraphBase(snapshotModel, this, poseControllers, this.graphSnapshotContexts)))
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
		
		PAnimationStagePlayback stage = getCurrentStage();
		if (stage == null)
		{
			this.state = ControllerState.STOP;
			return;
		}
		
		if (stage.isWaiting())
		{
			this.prevTime = this.time;
			this.time += tickCount;
			this.interpolationFrom = this.prevTime;
			this.interpolationTo = this.time;
			this.interpolationCycleLength = 0.0f;
			if (this.time >= stage.waitTicks())
				completePlayback();
			return;
		}
		
		PAnimation animation = model.animations().get(stage.animationName());
		if (animation == null)
		{
			completePlayback();
			return;
		}
		
		float length = animation.length();
		boolean includeReverseStart = !this.stageStarted || stage.speed() >= 0.0f;
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
				this.interpolationFrom = this.prevTime;
				this.interpolationTo = this.time;
				this.interpolationCycleLength = 0.0f;
				float bounded = Math.clamp(this.time, 0.0f, length);
				fireEvents(animatable, model, poseControllers, animation, this.prevTime, bounded, PAnimationType.PLAY_ONCE);
				if ((stage.speed() >= 0.0f && this.time >= length) || (stage.speed() < 0.0f && this.time <= 0.0f))
					completePlayback();
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
				this.interpolationFrom = this.prevTime;
				this.interpolationTo = this.time;
				this.interpolationCycleLength = 0.0f;
			}
			case CYCLE ->
			{
				if (length > 0)
				{
					double cyclicTime = this.time + (double)tickCount * stage.speed();
					this.interpolationFrom = this.time;
					this.interpolationTo = (float)cyclicTime;
					this.interpolationCycleLength = length;
					fireCyclicEvents(animatable, model, poseControllers, animation, this.prevTime, cyclicTime, includeReverseStart);
					double wrapped = cyclicTime % length;
					this.time = (float)(wrapped < 0.0 ? wrapped + length : wrapped);
				}
				else
				{
					this.time = nextTime;
					this.interpolationFrom = this.prevTime;
					this.interpolationTo = this.time;
					this.interpolationCycleLength = 0.0f;
				}
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
	                        double from,
	                        double to,
	                        PAnimationType type)
	{
		if (type == PAnimationType.CYCLE && animation.length() > 0.0f)
			fireCyclicEvents(animatable, model, poseControllers, animation, from, to, true);
		else if (to >= from)
			animation.eventsBetween((float)from, (float)to).forEach(event -> PAnimationEventDispatcher.dispatch(animatable, this, event, model, poseControllers));
		else
			animation.eventsBetweenReverse((float)from, (float)to).forEach(event -> PAnimationEventDispatcher.dispatch(animatable, this, event, model, poseControllers));
	}

	/**
	 * Fires the cyclic events.
	 * @param animatable the animatable to use.
	 * @param model the model to use.
	 * @param controllers the controllers to use.
	 * @param animation the animation to use.
	 * @param from the from to use.
	 * @param to the to to use.
	 * @param includeReverseStart whether to include the reverse departure marker.
	 */
	private void fireCyclicEvents(T animatable, PBakedModel model, Collection<PAnimationController<T>> controllers,
	                              PAnimation animation, double from, double to, boolean includeReverseStart)
	{
		animation.forEachCyclicEvent(from, to, includeReverseStart, event ->
				PAnimationEventDispatcher.dispatch(animatable, this, event, model, controllers));
	}

	/**
	 * Wraps a tick position into a non-negative cycle interval.
	 *
	 * @param value the value written by this operation
	 * @param modulus the modulus
	 * @return the tick position wrapped into the cycle interval
	 */
	private static float floorMod(float value, float modulus)
	{
		float result = value % modulus;
		return result < 0.0f ? result + modulus : result;
	}
	
	/**
	 * Advances the raw sequence to its next stage or stops when no stages remain.
	 */
	private void nextStage()
	{
		this.stageIndex++;
		enterCurrentStage();
	}

	/**
	 * Completes the playback currently provided by the active stage player.
	 */
	private void completePlayback()
	{
		if (this.stagePlayer == null)
		{
			this.state = ControllerState.STOP;
			return;
		}
		switch (this.stagePlayer.complete())
		{
			case NEXT_STAGE -> nextStage();
			case REPEAT_PLAYBACK -> resetPlayback(this.stagePlayer.currentPlayback());
		}
	}

	/**
	 * Creates the runtime player for the current recipe stage.
	 */
	private void enterCurrentStage()
	{
		if (this.currentAnimation == null || this.stageIndex >= this.currentAnimation.getStages().size())
		{
			this.stagePlayer = null;
			this.currentPlayback = null;
			this.state = ControllerState.STOP;
			return;
		}

		PIAnimationStage stage = this.currentAnimation.getStages().get(this.stageIndex);
		long stageSeed = this.randomSeed ^ ((long)this.stageIndex * 0x9E3779B97F4A7C15L);
		this.stagePlayer = Objects.requireNonNull(stage.createPlayer(new PAnimationStageContext(stageSeed)),
				"Animation stage player must not be null");
		resetPlayback(this.stagePlayer.currentPlayback());
	}

	/**
	 * Starts a resolved playback command from its initial time.
	 * @param playback the playback command to start.
	 */
	private void resetPlayback(PAnimationStagePlayback playback)
	{
		this.currentPlayback = Objects.requireNonNull(playback, "Animation stage playback must not be null");
		this.time = 0.0f;
		this.prevTime = 0.0f;
		this.interpolationFrom = 0.0f;
		this.interpolationTo = 0.0f;
		this.interpolationCycleLength = 0.0f;
		this.stageStarted = true;
	}
	
	/**
	 * Returns the render-time playback position in game ticks, accounting for cycle seams.
	 *
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the render-time playback position in game ticks, accounting for cycle seams
	 */
	public float getInterpolatedTime(float partialTick)
	{
		if (this.graphRuntime != null)
			return this.graphRuntime.interpolatedTime(partialTick);
		float interpolated = Mth.lerp(partialTick, this.interpolationFrom, this.interpolationTo);
		return this.interpolationCycleLength > 0.0f ? floorMod(interpolated, this.interpolationCycleLength) : interpolated;
	}
	
	/**
	 * Returns the current playback position in game ticks.
	 *
	 * @return the current playback position in game ticks
	 */
	public float getTime()
	{
		if (this.graphRuntime != null)
			return this.graphRuntime.time();
		return this.time;
	}
	
	/**
	 * Returns the normalized position within the active animation cycle.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @return the normalized position within the active animation cycle
	 */
	public float cyclePhase(PBakedModel model)
	{
		if (this.graphRuntime != null)
			return this.graphRuntime.cyclePhase(model);
		PAnimationStagePlayback stage = getCurrentStage();
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
		PAnimationStagePlayback stage = getCurrentStage();
		if (stage == null || stage.animationType() != PAnimationType.CYCLE)
			return;
		PAnimation animation = model.animations().get(stage.animationName());
		if (animation == null || animation.length() <= 0.0f)
			return;
		this.time = Math.clamp(phase, 0.0f, 1.0f) * animation.length();
		this.prevTime = this.time;
		this.interpolationFrom = this.time;
		this.interpolationTo = this.time;
		this.interpolationCycleLength = 0.0f;
		this.stageStarted = false;
	}

	/**
	 * Moves playback to the supplied animation time without advancing the owner tick count.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @param animationTime the animation playback position supplied to this operation
	 */
	public void seek(PBakedModel model, float animationTime)
	{
		if (this.graphRuntime != null)
			return;
		PAnimationStagePlayback stage = getCurrentStage();
		if (stage == null || stage.isWaiting())
			return;
		PAnimation animation = model.animations().get(stage.animationName());
		if (animation == null || animation.length() <= 0.0f)
			return;
		this.time = stage.animationType() == PAnimationType.CYCLE ? floorMod(animationTime, animation.length()) :
				Math.clamp(animationTime, 0.0f, animation.length());
		this.prevTime = this.time;
		this.interpolationFrom = this.time;
		this.interpolationTo = this.time;
		this.interpolationCycleLength = 0.0f;
		this.stageStarted = false;
	}
	
	/**
	 * Returns the playback currently resolved by the active stage player.
	 * @return the current playback, or {@code null} when no raw animation is active.
	 */
	public @Nullable PAnimationStagePlayback getCurrentStage()
	{
		return this.currentPlayback;
	}

	/**
	 * Returns the animation layers contributed by the active graph for the supplied model.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @return the animation layers contributed by the active graph for the supplied model
	 */
	public List<PAnimationGraphRuntime.Layer> graphLayers(PBakedModel model)
	{
		if (this.graphRuntime == null || this.state == ControllerState.STOP)
			return List.of();
		return this.graphRuntime.layers(model);
	}
	
	/**
	 * Chooses the playback state of a controller from the current animatable state.
	 *
	 * @param <T> the animatable owner type
	 */
	@FunctionalInterface
/**
 * Defines the contract for state handler.
 */
	public interface StateHandler<T extends PAnimatable<T>>
	{
		/**
		 * Evaluates the owner state and returns the requested controller playback state.
		 *
		 * @param state the state
		 * @return the playback state requested for this owner tick
		 */
		ControllerState handle(AnimatableState<T> state);
	}

	/**
	 * Returns the registration name of this controller.
	 *
	 * @return the registration name of this controller
	 */
	public String name()
	{
		return this.name;
	}
	
/**
	 * Owner, controller, tick and model values supplied to a controller state handler.
	 *
	 * @param <T> the animatable owner type
	 * @param animatable the owner whose animation state is being processed
	 * @param controller the controller supplying playback and persistent expression state
	 */
	public record AnimatableState<T extends PAnimatable<T>>(T animatable, PAnimationController<T> controller)
	{
	}
}
