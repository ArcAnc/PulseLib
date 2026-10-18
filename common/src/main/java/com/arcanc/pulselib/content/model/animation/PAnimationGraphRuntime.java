/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.data.gecko.MolangParser;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Per-controller state machine that advances graph playback, blends transitions and schedules one-shot
 * overlays.
 */
public final class PAnimationGraphRuntime
{
	private final PAnimationGraph graph;
	private final PAnimationParameters parameters = new PAnimationParameters();
	private Playback current;
	private @Nullable Transition transition;
	private final Map<Integer, OverlayPlayback> overlays = new LinkedHashMap<>();

	/**
	 * Creates independent playback, parameter and transition state for the supplied graph.
	 *
	 * @param graph the immutable animation state graph
	 */
	public PAnimationGraphRuntime(PAnimationGraph graph)
	{
		this.graph = Objects.requireNonNull(graph);
		this.current = new Playback(graph.initialState());
	}

	/**
	 * Returns the immutable graph evaluated by this runtime.
	 *
	 * @return the immutable graph evaluated by this runtime
	 */
	public PAnimationGraph graph()
	{
		return this.graph;
	}

	/**
	 * Returns the mutable parameters and triggers used to evaluate graph conditions.
	 *
	 * @return the mutable parameters and triggers used to evaluate graph conditions
	 */
	public PAnimationParameters parameters()
	{
		return this.parameters;
	}

	/**
	 * Returns the index of the active base state.
	 *
	 * @return the index of the active base state
	 */
	public int stateIndex()
	{
		return this.transition == null ? this.current.stateIndex : this.transition.target.stateIndex;
	}

	/**
	 * Returns the active base animation state.
	 *
	 * @return the active base animation state
	 */
	public PAnimationState state()
	{
		return this.graph.states().get(stateIndex());
	}

	/**
	 * Returns whether a blend between base states is active.
	 *
	 * @return whether a blend between base states is active
	 */
	public boolean isTransitioning()
	{
		return this.transition != null;
	}

	/**
	 * Returns the speed of an active overlay identified by its unique graph state name.
	 *
	 * @param stateName the graph state name
	 * @return the speed of an active overlay identified by its unique graph state name
	 */
	public float overlaySpeed(String stateName)
	{
		return overlaySpeed(overlayIndex(stateName));
	}

	/**
	 * Returns the speed of an active overlay. Unknown indices and inactive states fail explicitly.
	 *
	 * @param stateIndex the graph state index
	 * @return the speed of an active overlay. Unknown indices and inactive states fail explicitly
	 */
	public float overlaySpeed(int stateIndex)
	{
		return activeOverlay(stateIndex).speed;
	}

	/**
	 * Changes signed clip speed without seeking. Negative plays backwards; zero pauses. Fade clocks continue.
	 *
	 * @param stateName the graph state name
	 * @param speed the playback speed multiplier
	 */
	public void setOverlaySpeed(String stateName, float speed)
	{
		setOverlaySpeed(overlayIndex(stateName), speed);
	}

	/**
	 * Changes the speed of the active definition at this graph state index.
	 *
	 * @param stateIndex the graph state index
	 * @param speed the playback speed multiplier
	 */
	public void setOverlaySpeed(int stateIndex, float speed)
	{
		if (!Float.isFinite(speed))
		{
			throw new IllegalArgumentException("Overlay speed must be finite: " + speed);
		}
		OverlayPlayback overlay = activeOverlay(stateIndex);
		if (overlay.speed != speed)
		{
			overlay.playback.previousTime = overlay.playback.time;
			if (overlay.cancelElapsed < 0.0f)
			{
				overlay.frozenPose = null;
			}
		}
		if (speed != 0.0f && overlay.reverse != (speed < 0.0f))
		{
			if (overlay.cancelElapsed < 0.0f)
			{
				boolean fading = overlay.fadeElapsed >= 0.0f || overlay.recoveryElapsed >= 0.0f &&
						overlay.recoveryElapsed < overlay.overlay.fadeOutDuration();
				if (fading)
				{
					overlay.recoveryWeight = overlayWeight(overlay);
					overlay.recoveryElapsed = 0.0f;
					overlay.previousRecoveryElapsed = 0.0f;
				}
				overlay.fadeElapsed = -1.0f;
				overlay.previousFadeElapsed = -1.0f;
			}
			overlay.reverse = speed < 0.0f;
		}
		overlay.speed = speed;
	}

	/**
	 * Restarts an active overlay, retaining its speed. Returns false for an inactive definition.
	 *
	 * @param stateName the graph state name
	 * @return whether an active overlay was found and restarted
	 */
	public boolean restartOverlay(String stateName)
	{
		return restartOverlay(overlayIndex(stateName));
	}

	/**
	 * Restarts at the playback direction's entry boundary, retaining that direction while paused. Cancels
	 * fade-out.
	 *
	 * @param stateIndex the graph state index
	 * @return whether an active overlay was found and restarted
	 */
	public boolean restartOverlay(int stateIndex)
	{
		overlayDefinition(stateIndex);
		OverlayPlayback overlay = this.overlays.get(stateIndex);
		if (overlay == null)
		{
			return false;
		}
		overlay.restart();
		return true;
	}

	/**
	 * Cancels an active overlay using its fade-out duration, including paused playback.
	 *
	 * @param stateName the graph state name
	 * @return whether an active overlay was found to stop
	 */
	public boolean stopOverlay(String stateName)
	{
		return stopOverlay(overlayIndex(stateName));
	}

	/**
	 * Starts one fade-out, or removes the instance immediately when its duration is zero.
	 *
	 * @param stateIndex the graph state index
	 * @return whether an active overlay was found to stop
	 */
	public boolean stopOverlay(int stateIndex)
	{
		overlayDefinition(stateIndex);
		OverlayPlayback overlay = this.overlays.get(stateIndex);
		if (overlay == null)
		{
			return false;
		}
		if (overlay.overlay.fadeOutDuration() == 0.0f)
		{
			this.overlays.remove(stateIndex);
		}
		else if (overlay.cancelElapsed < 0.0f)
		{
			overlay.cancelWeight = overlayWeight(overlay);
			overlay.cancelElapsed = 0.0f;
			overlay.previousCancelElapsed = 0.0f;
		}
		return true;
	}

	private int overlayIndex(String stateName)
	{
		int found = -1;
		for (int index = 0; index < this.graph.states().size(); index++)
		{
			PAnimationState state = this.graph.states().get(index);
			if (state.isOverlay() && state.name().equals(stateName))
			{
				if (found >= 0)
				{
					throw new IllegalArgumentException("Ambiguous overlay state name: " + stateName + "; use its state index");
				}
				found = index;
			}
		}
		if (found < 0)
		{
			throw new IllegalArgumentException("Unknown overlay graph state: " + stateName);
		}
		return found;
	}

	private PAnimationState.OneShotOverlay overlayDefinition(int stateIndex)
	{
		if (stateIndex < 0 || stateIndex >= this.graph.states().size() ||
				!(this.graph.states().get(stateIndex) instanceof PAnimationState.OneShotOverlay overlay))
		{
			throw new IllegalArgumentException("Not an overlay graph state index: " + stateIndex);
		}
		return overlay;
	}

	private OverlayPlayback activeOverlay(int stateIndex)
	{
		PAnimationState.OneShotOverlay definition = overlayDefinition(stateIndex);
		OverlayPlayback overlay = this.overlays.get(stateIndex);
		if (overlay == null)
		{
			throw new IllegalStateException("Overlay graph state is inactive: " + definition.name());
		}
		return overlay;
	}

	/**
	 * Restores the initial graph state and clears transition and overlay playback.
	 */
	public void reset()
	{
		this.current = new Playback(this.graph.initialState());
		this.transition = null;
		this.overlays.clear();
	}

	/**
	 * Advances graph playback by the supplied tick delta and reports crossed event intervals.
	 *
	 * @param model the baked model supplying indexed bones and animations; may be {@code null}
	 * @param delta the elapsed playback interval in game ticks
	 * @return the clip event intervals crossed during this update
	 */
	public List<EventTrack> tick(@Nullable PBakedModel model, float delta)
	{
		return tickPrecise(model, delta).stream().
				map(track -> new EventTrack(track.animation(), (float)track.from(), (float)track.to(), track.animationType())).
				toList();
	}

	/**
	 * Advances the graph once and retains double precision for event dispatch.
	 * The existing tick/EventTrack API remains a float view for its callers.
	 * @param model the model used for phase/transition evaluation.
	 * @param delta the elapsed ticks.
	 * @return precise event windows for this tick.
	 */
	public List<PreciseEventTrack> tickPrecise(@Nullable PBakedModel model, float delta)
	{
		return tickPrecise(model, delta, (boundModel, runtime) -> runtime.sampleBasePose(boundModel, 1.0f,
				new MolangParser.Context().query("anim_time", PAnimationEvaluationContext.ticksToSeconds(runtime.interpolatedTime(1.0f))), boundModel.bindPose()));
	}

	/**
	 * Advances once. RESTART samples the base at the current tick boundary (partialTick = 1).
	 * The sampler must resolve this runtime's base only, using current context and accumulated
	 * preceding controllers. It must exclude this runtime's independent overlays.
	 *
	 * @param model the baked model supplying indexed bones and animations; may be {@code null}
	 * @param delta the elapsed playback interval in game ticks
	 * @param snapshotSampler the snapshot sampler
	 * @return the double-precision clip event intervals crossed during this update
	 */
	public List<PreciseEventTrack> tickPrecise(@Nullable PBakedModel model, float delta,
	                                        BiFunction<PBakedModel, PAnimationGraphRuntime, PPose> snapshotSampler)
	{
		if (!Float.isFinite(delta))
		{
			throw new IllegalArgumentException("Animation graph tick delta must be finite");
		}
		List<PreciseEventTrack> eventTracks = new ArrayList<>();
		float elapsed = Math.max(delta, 0.0f);
		if (this.transition == null)
		{
			advance(this.current, elapsed, eventTracks);
		}
		else
		{
			for (WeightedPlayback source : this.transition.sources)
			{
				advance(source.playback, elapsed, eventTracks);
			}
			advance(this.transition.target, elapsed, eventTracks);
			this.transition.previousElapsed = this.transition.elapsed;
			this.transition.elapsed += elapsed;
			if (this.transition.elapsed >= this.transition.duration)
			{
				this.current = this.transition.target;
				this.transition = null;
			}
		}
		for (OverlayPlayback overlay : this.overlays.values())
		{
			if (model != null)
			{
				requireOverlayClip(overlay.overlay, model);
			}
			advanceOverlay(overlay, elapsed, eventTracks);
		}
		this.overlays.values().removeIf(overlay -> overlayFinished(overlay) && !this.parameters.isTriggered(overlay.overlay.trigger()));

		triggerOverlays(model);
		PAnimationTransition candidate = selectTransition(model);
		if (candidate != null)
		{
			candidate.condition().consume(this.parameters);
			beginTransition(candidate, model, snapshotSampler);
		}
		return List.copyOf(eventTracks);
	}

	/**
	 * Returns live clip layers at the current tick; frozen sources are available through samplePose.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @return live clip layers at the current tick; frozen sources are available through samplePose
	 */
	public List<Layer> layers(PBakedModel model)
	{
		return layers(model, 1.0f);
	}

	/**
	 * Returns interpolated live clip layers, preserving the original Layer record API.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return interpolated live clip layers, preserving the original Layer record API
	 */
	public List<Layer> layers(PBakedModel model, float partialTick)
	{
		List<Layer> result = baseLayers(model, partialTick);
		result.addAll(overlayLayers(model, partialTick));
		return List.copyOf(result);
	}

	private List<Layer> baseLayers(PBakedModel model, float partialTick)
	{
		List<Layer> result = new ArrayList<>();
		if (this.transition == null)
		{
			appendStateLayers(result, this.current, 1.0f, false, model, partialTick);
		}
		else
		{
			float alpha = transitionAlpha(this.transition, partialTick);
			for (WeightedPlayback source : this.transition.sources)
			{
				appendStateLayers(result, source.playback, source.weight * (1.0f - alpha), false, model, partialTick);
			}
			appendStateLayers(result, this.transition.target, alpha, false, model, partialTick);
		}
		return result;
	}

	private List<Layer> overlayLayers(PBakedModel model, float partialTick)
	{
		List<Layer> result = new ArrayList<>();
		for (OverlayPlayback overlay : this.overlays.values())
		{
			requireOverlayClip(overlay.overlay, model);
			float weight = overlayWeight(overlay, partialTick);
			if (weight > 0.0f)
			{
				result.add(new Layer(overlay.overlay.animation(), (float)playbackTime(overlay.playback, partialTick),
						overlay.overlay.interpolation(), weight, true));
			}
		}
		return result;
	}

	/**
	 * Resolves this controller's local animation delta, including independent overlays exactly once.
	 * The accumulated pose provides Molang this-values; it is never captured as another controller's source.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param context the Molang context containing current frame and instance values
	 * @param accumulatedPose the pose contributed by preceding controllers
	 * @return the indexed bone pose
	 */
	public PPose samplePose(PBakedModel model, float partialTick, MolangParser.Context context, PPose accumulatedPose)
	{
		PPose base = sampleBasePose(model, partialTick, context, accumulatedPose);
		for (OverlayPlayback overlay : this.overlays.values())
		{
			requireOverlayClip(overlay.overlay, model);
			float weight = overlayWeight(overlay, partialTick);
			if (weight <= 0.0f)
			{
				continue;
			}
			boolean frozen = overlay.speed == 0.0f || overlay.cancelElapsed >= 0.0f;
			PPose delta;
			if (frozen && overlay.frozenPose != null)
			{
				delta = overlay.frozenPose.sample();
			}
			else
			{
				float time = (float)(frozen ? overlay.playback.time : playbackTime(overlay.playback, partialTick));
				delta = PAnimationRuntime.sampleGraphLayers(model, List.of(new Layer(overlay.overlay.animation(), time,
						overlay.overlay.interpolation(), 1.0f, false)), context, accumulatedPose);
				if (frozen)
				{
					overlay.frozenPose = new PPoseSnapshot(delta);
				}
			}
			PAnimationRuntime.applyGraphOverlay(base, delta, weight);
		}
		return base;
	}

	/**
	 * Resolves only the base delta at the requested time, before independent overlays.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param context the Molang context containing current frame and instance values
	 * @param accumulatedPose the pose contributed by preceding controllers
	 * @return the indexed bone pose
	 */
	public PPose sampleBasePose(PBakedModel model, float partialTick, MolangParser.Context context, PPose accumulatedPose)
	{
		if (this.transition == null || this.transition.sourcePose == null)
		{
			return PAnimationRuntime.sampleGraphLayers(model, baseLayers(model, partialTick), context, accumulatedPose);
		}
		return sampleTransition(this.transition.sourcePose, this.transition.target,
				transitionAlpha(this.transition, partialTick), model, partialTick, context, accumulatedPose);
	}

	private PPose sampleTransition(BasePoseSource source, Playback target, float alpha, PBakedModel model,
	                              float partialTick, MolangParser.Context context, PPose accumulatedPose)
	{
		PPose sourcePose = source.sample(model, partialTick, context, accumulatedPose);
		List<Layer> targetLayers = new ArrayList<>();
		appendStateLayers(targetLayers, target, 1.0f, false, model, partialTick);
		PPose targetPose = PAnimationRuntime.sampleGraphLayers(model, targetLayers, context, accumulatedPose);
		return PPoseMixer.blend(new PPose(model.boneCount()), sourcePose, targetPose, alpha);
	}

	private static float transitionAlpha(Transition transition, float partialTick)
	{
		float elapsed = transition.previousElapsed + (transition.elapsed - transition.previousElapsed) * Math.clamp(partialTick, 0.0f, 1.0f);
		return transition.duration <= 0.0f ? 1.0f : Math.clamp(elapsed / transition.duration, 0.0f, 1.0f);
	}

	private static double playbackTime(Playback playback, float partialTick)
	{
		return playback.previousTime + (playback.time - playback.previousTime) * Math.clamp(partialTick, 0.0f, 1.0f);
	}

	/**
	 * Returns the normalized playback phase of the active base cycle.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @return the normalized playback phase of the active base cycle
	 */
	public float cyclePhase(PBakedModel model)
	{
		Playback playback = this.transition == null ? this.current : this.transition.target;
		if (this.graph.states().get(playback.stateIndex).animationType() != PAnimationType.CYCLE)
		{
			return Float.NaN;
		}
		return phase(playback, model);
	}

	/**
	 * Returns the base-state playback position interpolated between game ticks.
	 *
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the base-state playback position interpolated between game ticks
	 */
	public float interpolatedTime(float partialTick)
	{
		Playback playback = this.transition == null ? this.current : this.transition.target;
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		float alpha = Math.clamp(partialTick, 0.0f, 1.0f);
		return (float)((playback.previousTime + (playback.time - playback.previousTime) * alpha) * state.speed());
	}

	/**
	 * Returns the current base-state playback position in game ticks.
	 *
	 * @return the current base-state playback position in game ticks
	 */
	public float time()
	{
		return (float)(this.transition == null ? this.current : this.transition.target).time;
	}

	/**
	 * Aligns the active base playback with the supplied normalized cycle phase.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @param phase the normalized animation cycle phase
	 */
	public void syncCycle(PBakedModel model, float phase)
	{
		float clampedPhase = Math.clamp(phase, 0.0f, 1.0f);
		syncPlayback(this.current, clampedPhase, model);
		if (this.transition != null)
		{
			for (WeightedPlayback source : this.transition.sources)
			{
				syncPlayback(source.playback, clampedPhase, model);
			}
			syncPlayback(this.transition.target, clampedPhase, model);
		}
	}

	private void triggerOverlays(@Nullable PBakedModel model)
	{
		Set<String> triggered = new LinkedHashSet<>();
		for (PAnimationState state : this.graph.states())
		{
			if (state instanceof PAnimationState.OneShotOverlay overlay && this.parameters.isTriggered(overlay.trigger()))
			{
				requireOverlayClip(overlay, model);
				triggered.add(overlay.trigger());
			}
		}
		for (int index = 0; index < this.graph.states().size(); index++)
		{
			PAnimationState state = this.graph.states().get(index);
			if (state instanceof PAnimationState.OneShotOverlay overlay && triggered.contains(overlay.trigger()))
			{
				OverlayPlayback active = this.overlays.get(index);
				if (active == null)
				{
					this.overlays.put(index, new OverlayPlayback(new Playback(index), overlay,
							requireOverlayClip(overlay, model).length()));
				}
				else
				{
					active.restart();
				}
			}
		}
		for (String trigger : triggered)
		{
			this.parameters.consumeTrigger(trigger);
		}
	}

	private PAnimation requireOverlayClip(PAnimationState.OneShotOverlay overlay, @Nullable PBakedModel model)
	{
		PAnimation animation = model == null ? null : model.animations().get(overlay.animation());
		if (animation == null)
		{
			throw new IllegalStateException("Overlay graph state '" + overlay.name() + "' requires missing animation clip '" +
					overlay.animation() + "'" + (model == null ? " (no model is bound)" : ""));
		}
		return animation;
	}

	private @Nullable PAnimationTransition selectTransition(@Nullable PBakedModel model)
	{
		if (this.transition != null && this.transition.policy == PInterruptionPolicy.COMPLETE_CURRENT)
		{
			return null;
		}
		int source = stateIndex();
		float phase = phase(this.transition == null ? this.current : this.transition.target, model);
		return this.graph.transitions().stream().
				filter(transition -> transition.source() == source).
				filter(transition -> transition.exitTime() < 0.0f || phase >= transition.exitTime()).
				filter(transition -> transition.condition().test(this.parameters)).
				max(Comparator.comparingInt(PAnimationTransition::priority)).
				orElse(null);
	}

	private void beginTransition(PAnimationTransition request, @Nullable PBakedModel model,
	                             BiFunction<PBakedModel, PAnimationGraphRuntime, PPose> snapshotSampler)
	{
		List<WeightedPlayback> sources;
		BasePoseSource sourcePose = null;
		if (request.interruption() == PInterruptionPolicy.RESTART && request.blendDuration() > 0.0f)
		{
			if (model == null)
			{
				throw new IllegalStateException("RESTART requires a bound model to capture graph state '" + state().name() + "'");
			}
			PPoseSnapshot snapshot = new PPoseSnapshot(snapshotSampler.apply(model, this));
			sourcePose = (boundModel, partialTick, context, accumulatedPose) -> snapshot.sample();
			sources = List.of();
		}
		else if (this.transition == null)
		{
			sources = List.of(new WeightedPlayback(this.current, 1.0f));
		}
		else if (request.interruption() == PInterruptionPolicy.FROM_CURRENT)
		{
			float alpha = this.transition.duration <= 0.0f ? 1.0f : Math.clamp(this.transition.elapsed / this.transition.duration, 0.0f, 1.0f);
			sources = new ArrayList<>(this.transition.sources.size() + 1);
			for (WeightedPlayback source : this.transition.sources)
			{
				sources.add(new WeightedPlayback(source.playback, source.weight * (1.0f - alpha)));
			}
			sources.add(new WeightedPlayback(this.transition.target, alpha));
			if (this.transition.sourcePose != null)
			{
				BasePoseSource oldSource = this.transition.sourcePose;
				Playback oldTarget = this.transition.target;
				sourcePose = (boundModel, partialTick, context, accumulatedPose) ->
						sampleTransition(oldSource, oldTarget, alpha, boundModel, partialTick, context, accumulatedPose);
			}
		}
		else if (request.interruption() == PInterruptionPolicy.RESTART)
		{
			sources = List.of();
		}
		else
		{
			return;
		}

		Playback target = new Playback(request.target());
		PAnimationState targetState = this.graph.states().get(request.target());
		if (targetState.synchronizedCycle())
		{
			syncPlayback(target, phase(this.transition == null ? this.current : this.transition.target, model), model);
		}
		this.transition = new Transition(sources, sourcePose, target, request.blendDuration(), request.interruption());
		if (request.blendDuration() == 0.0f)
		{
			this.current = target;
			this.transition = null;
		}
	}

	private void appendStateLayers(List<Layer> result, Playback playback, float stateWeight, boolean overlay, PBakedModel model, float partialTick)
	{
		if (stateWeight <= 0.0f)
		{
			return;
		}
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		for (PAnimationSample sample : state.samples(this.parameters))
			if (sample.weight() > 0.0f)
				result.add(new Layer(sample.animation(), sampleTime(playback, state, sample.animation(), model, partialTick),
						state.interpolation(), stateWeight * sample.weight(), overlay));
	}

	private float sampleTime(Playback playback, PAnimationState state, String animationName, PBakedModel model, float partialTick)
	{
		PAnimation animation = model.animations().get(animationName);
		if (animation == null || animation.length() <= 0.0f)
		{
			return (float)(playbackTime(playback, partialTick) * state.speed());
		}
		double time = playbackTime(playback, partialTick) * state.speed();
		if (state.synchronizedCycle() && state.animationType() == PAnimationType.CYCLE)
		{
			time = phase(playback, model, partialTick) * animation.length();
		}
		if (state.animationType() == PAnimationType.CYCLE)
		{
			return (float)(time % animation.length());
		}
		return (float)Math.min(time, animation.length());
	}

	private float phase(Playback playback, @Nullable PBakedModel model)
	{
		return phase(playback, model, 1.0f);
	}

	private float phase(Playback playback, @Nullable PBakedModel model, float partialTick)
	{
		if (model == null)
		{
			return 0.0f;
		}
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		PAnimationSample sample = state.samples(this.parameters).stream().findFirst().orElse(null);
		if (sample == null)
		{
			return 0.0f;
		}
		PAnimation animation = model.animations().get(sample.animation());
		if (animation == null || animation.length() <= 0.0f)
		{
			return 0.0f;
		}
		double raw = playbackTime(playback, partialTick) * state.speed();
		return (float)(state.animationType() == PAnimationType.CYCLE ? (raw % animation.length()) / animation.length() :
				Math.clamp(raw / animation.length(), 0.0, 1.0));
	}

	private void syncPlayback(Playback playback, float phase, @Nullable PBakedModel model)
	{
		if (model == null)
		{
			return;
		}
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		if (!state.synchronizedCycle() || state.animationType() != PAnimationType.CYCLE || state.speed() == 0.0f)
		{
			return;
		}
		PAnimationSample sample = state.samples(this.parameters).stream().findFirst().orElse(null);
		PAnimation animation = sample == null ? null : model.animations().get(sample.animation());
		if (animation == null || animation.length() <= 0.0f)
		{
			return;
		}
		playback.time = (double)phase * animation.length() / state.speed();
		playback.previousTime = playback.time;
	}

	private boolean overlayFinished(OverlayPlayback overlay)
	{
		if (overlay.cancelElapsed >= 0.0f)
		{
			return overlay.cancelElapsed >= overlay.overlay.fadeOutDuration();
		}
		return overlay.speed > 0.0f && overlay.playback.time >= overlay.length ||
				overlay.speed < 0.0f && overlay.playback.time <= 0.0;
	}

	private float overlayWeight(OverlayPlayback overlay)
	{
		return overlayWeight(overlay, 1.0f);
	}

	private float overlayWeight(OverlayPlayback overlay, float partialTick)
	{
		float alpha = Math.clamp(partialTick, 0.0f, 1.0f);
		if (overlay.cancelElapsed >= 0.0f)
		{
			float elapsed = overlay.previousCancelElapsed + (overlay.cancelElapsed - overlay.previousCancelElapsed) * alpha;
			return overlay.cancelWeight * Math.max(0.0f, 1.0f - elapsed / overlay.overlay.fadeOutDuration());
		}
		float weight = overlay.overlay.fadeInDuration() == 0.0f ? 1.0f :
				(float)Math.min((overlay.previousElapsed + (overlay.elapsed - overlay.previousElapsed) * alpha) / overlay.overlay.fadeInDuration(), 1.0);
		if (overlay.recoveryElapsed >= 0.0f)
		{
			float elapsed = overlay.previousRecoveryElapsed + (overlay.recoveryElapsed - overlay.previousRecoveryElapsed) * alpha;
			float recovery = Math.clamp(elapsed / overlay.overlay.fadeOutDuration(), 0.0f, 1.0f);
			weight = Math.min(weight, overlay.recoveryWeight + (1.0f - overlay.recoveryWeight) * recovery);
		}
		if (overlay.fadeElapsed >= 0.0f)
		{
			float from = Math.max(0.0f, overlay.previousFadeElapsed);
			float elapsed = from + (overlay.fadeElapsed - from) * alpha;
			weight = Math.min(weight, Math.max(0.0f, 1.0f - elapsed / overlay.overlay.fadeOutDuration()));
		}
		return weight;
	}

	private void advanceOverlay(OverlayPlayback overlay, float delta, List<PreciseEventTrack> tracks)
	{
		overlay.previousElapsed = overlay.elapsed;
		overlay.previousFadeElapsed = overlay.fadeElapsed;
		overlay.previousCancelElapsed = overlay.cancelElapsed;
		overlay.previousRecoveryElapsed = overlay.recoveryElapsed;
		overlay.elapsed += delta;
		overlay.playback.previousTime = overlay.playback.time;
		if (overlay.cancelElapsed >= 0.0f)
		{
			overlay.cancelElapsed += delta;
			return;
		}
		if (overlay.fadeElapsed >= 0.0f)
		{
			overlay.fadeElapsed += delta;
		}
		if (overlay.recoveryElapsed >= 0.0f)
		{
			overlay.recoveryElapsed = Math.min(overlay.recoveryElapsed + delta, overlay.overlay.fadeOutDuration());
		}
		if (overlay.speed == 0.0f || delta == 0.0f)
		{
			return;
		}
		double from = overlay.playback.time;
		overlay.playback.time = Math.clamp(from + (double)delta * overlay.speed, 0.0, (double)overlay.length);
		if (overlay.playback.time != from)
		{
			tracks.add(new PreciseEventTrack(overlay.overlay.animation(), from, overlay.playback.time, PAnimationType.PLAY_ONCE));
		}
		if (overlay.fadeElapsed < 0.0f && overlay.overlay.fadeOutDuration() > 0.0f)
		{
			double distance = overlay.reverse ? overlay.playback.time : overlay.length - overlay.playback.time;
			double remaining = distance / Math.abs(overlay.speed);
			if (remaining <= overlay.overlay.fadeOutDuration())
			{
				overlay.fadeElapsed = (float)(overlay.overlay.fadeOutDuration() - remaining);
			}
		}
	}

	private void advance(Playback playback, float delta, List<PreciseEventTrack> tracks)
	{
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		playback.previousTime = playback.time;
		double nextTime = playback.time + delta;
		if (!Double.isFinite(nextTime) || delta > 0.0f && nextTime <= playback.time)
		{
			throw new IllegalArgumentException("Animation graph time cannot advance precisely: state=" +
					state.name() + ", time=" + playback.time + ", delta=" + delta);
		}
		playback.time = nextTime;
		if (state.speed() == 0.0f || delta == 0.0f)
		{
			return;
		}
		double from = playback.previousTime * state.speed();
		double to = playback.time * state.speed();
		for (PAnimationSample sample : state.samples(this.parameters))
			if (sample.weight() > 0.0f)
			{
				tracks.add(new PreciseEventTrack(sample.animation(), from, to, state.animationType()));
			}
	}

	/**
	 * Weighted animation contribution sampled from a graph state.
	 *
	 * @param animation the source animation clip name
	 * @param time the animation position in game ticks
	 * @param interpolation the keyframe interpolation policy
	 * @param weight the contribution weight
	 * @param overlay the packed overlay texture coordinates
	 */
	public record Layer(String animation, float time, PInterpolationType interpolation, float weight, boolean overlay)
	{
	}

	/**
	 * Animation event interval crossed while a graph layer advances.
	 *
	 * @param animation the source animation clip name
	 * @param from the preceding animation tick position
	 * @param to the current animation tick position
	 * @param animationType the animation loop and completion policy
	 */
	public record EventTrack(String animation, float from, float to, PAnimationType animationType)
	{
	}

	/**
	 * A continuous event window; never round this back to float before dispatch.
	 *
	 * @param animation the source animation clip name
	 * @param from the preceding animation tick position
	 * @param to the current animation tick position
	 * @param animationType the animation loop and completion policy
	 */
	public record PreciseEventTrack(String animation, double from, double to, PAnimationType animationType)
	{
	}

	/**
	 * Clock and sampled clips belonging to one graph-state playback.
	 */
	private static final class Playback
	{
		private final int stateIndex;
		private double time;
		private double previousTime;

		private Playback(int stateIndex)
		{
			this.stateIndex = stateIndex;
		}
	}

	/**
	 * Graph playback paired with its current blend weight.
	 *
	 * @param playback the playback
	 * @param weight the contribution weight
	 */
	private record WeightedPlayback(Playback playback, float weight)
	{
	}

	/**
	 * Samples the base pose needed to capture or blend a graph transition.
	 */
	@FunctionalInterface
	private interface BasePoseSource
	{
		/**
		 * Samples the graph base pose using the supplied model and accumulated controller pose.
		 *
		 * @param model the baked model supplying indexed bones and animations
		 * @param partialTick the interpolation fraction between the previous and current game ticks
		 * @param context the Molang context containing current frame and instance values
		 * @param accumulatedPose the pose contributed by preceding controllers
		 * @return the indexed bone pose
		 */
		PPose sample(PBakedModel model, float partialTick, MolangParser.Context context, PPose accumulatedPose);
	}

	/**
	 * Active blend between a source pose and a destination graph playback.
	 */
	private static final class Transition
	{
		private final List<WeightedPlayback> sources;
		private final @Nullable BasePoseSource sourcePose;
		private final Playback target;
		private final float duration;
		private final PInterruptionPolicy policy;
		private float elapsed;
		private float previousElapsed;

		private Transition(List<WeightedPlayback> sources, @Nullable BasePoseSource sourcePose, Playback target, float duration, PInterruptionPolicy policy)
		{
			this.sources = List.copyOf(sources);
			this.sourcePose = sourcePose;
			this.target = target;
			this.duration = duration;
			this.policy = policy;
		}
	}

	/**
	 * Clock and fade state of an active one-shot animation overlay.
	 */
	private static final class OverlayPlayback
	{
		private final Playback playback;
		private final PAnimationState.OneShotOverlay overlay;
		private final float length;
		private float speed;
		private boolean reverse;
		private double elapsed;
		private double previousElapsed;
		private float previousFadeElapsed = -1.0f;
		private float previousCancelElapsed = -1.0f;
		private @Nullable PPoseSnapshot frozenPose;
		private float fadeElapsed = -1.0f;
		private float cancelElapsed = -1.0f;
		private float cancelWeight;
		private float recoveryElapsed = -1.0f;
		private float previousRecoveryElapsed = -1.0f;
		private float recoveryWeight;

		private OverlayPlayback(Playback playback, PAnimationState.OneShotOverlay overlay, float length)
		{
			this.playback = playback;
			this.overlay = overlay;
			this.length = length;
			this.speed = overlay.speed();
			this.reverse = this.speed < 0.0f;
			restart();
		}

		private void restart()
		{
			this.playback.time = this.reverse ? this.length : 0.0;
			this.playback.previousTime = this.playback.time;
			this.elapsed = 0.0;
			this.previousElapsed = 0.0;
			this.previousFadeElapsed = -1.0f;
			this.previousCancelElapsed = -1.0f;
			this.frozenPose = null;
			this.fadeElapsed = -1.0f;
			this.cancelElapsed = -1.0f;
			this.cancelWeight = 0.0f;
			this.recoveryElapsed = -1.0f;
			this.previousRecoveryElapsed = -1.0f;
			this.recoveryWeight = 0.0f;
		}
	}
}
