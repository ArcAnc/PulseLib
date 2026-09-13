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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Provides support for animation graph runtime.
 */
public final class PAnimationGraphRuntime
{
	private final PAnimationGraph graph;
	private final PAnimationParameters parameters = new PAnimationParameters();
	private Playback current;
	private @Nullable Transition transition;
	private final List<OverlayPlayback> overlays = new ArrayList<>();

	/**
	 * Creates an instance of the enclosing type.
	 * @param graph the graph to use.
	 */
	public PAnimationGraphRuntime(PAnimationGraph graph)
	{
		this.graph = Objects.requireNonNull(graph);
		this.current = new Playback(graph.initialState());
	}

	/**
	 * Performs the graph operation.
	 * @return the value produced by this operation.
	 */
	public PAnimationGraph graph()
	{
		return this.graph;
	}

	/**
	 * Performs the parameters operation.
	 * @return the value produced by this operation.
	 */
	public PAnimationParameters parameters()
	{
		return this.parameters;
	}

	/**
	 * Performs the state index operation.
	 * @return the value produced by this operation.
	 */
	public int stateIndex()
	{
		return this.transition == null ? this.current.stateIndex : this.transition.target.stateIndex;
	}

	/**
	 * Performs the state operation.
	 * @return the value produced by this operation.
	 */
	public PAnimationState state()
	{
		return this.graph.states().get(stateIndex());
	}

	/**
	 * Determines whether transitioning.
	 * @return the value produced by this operation.
	 */
	public boolean isTransitioning()
	{
		return this.transition != null;
	}

	/**
	 * Performs the reset operation.
	 */
	public void reset()
	{
		this.current = new Playback(this.graph.initialState());
		this.transition = null;
		this.overlays.clear();
	}
	
	/**
	 * Performs the tick operation.
	 * @param model the model to use.
	 * @param delta the delta to use.
	 * @return the value produced by this operation.
	 */
	public List<EventTrack> tick(@Nullable PBakedModel model, float delta)
	{
		if (!Float.isFinite(delta))
			throw new IllegalArgumentException("Animation graph tick delta must be finite");
		List<EventTrack> eventTracks = new ArrayList<>();
		float elapsed = Math.max(delta, 0.0f);
		if (this.transition == null)
			advance(this.current, elapsed, eventTracks);
		else
		{
			for (WeightedPlayback source : this.transition.sources)
				advance(source.playback, elapsed, eventTracks);
			advance(this.transition.target, elapsed, eventTracks);
			this.transition.elapsed += elapsed;
			if (this.transition.elapsed >= this.transition.duration)
			{
				this.current = this.transition.target;
				this.transition = null;
			}
		}
		for (OverlayPlayback overlay : this.overlays)
			advance(overlay.playback, elapsed, eventTracks);
		this.overlays.removeIf(overlay -> overlayFinished(overlay, model));

		triggerOverlays();
		PAnimationTransition candidate = selectTransition(model);
		if (candidate != null)
		{
			candidate.condition().consume(this.parameters);
			beginTransition(candidate, model);
		}
		return List.copyOf(eventTracks);
	}
	
	/**
	 * Performs the layers operation.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	public List<Layer> layers(PBakedModel model)
	{
		List<Layer> result = new ArrayList<>();
		if (this.transition == null)
			appendStateLayers(result, this.current, 1.0f, false, model);
		else
		{
			float alpha = this.transition.duration <= 0.0f ? 1.0f : Math.clamp(this.transition.elapsed / this.transition.duration, 0.0f, 1.0f);
			for (WeightedPlayback source : this.transition.sources)
				appendStateLayers(result, source.playback, source.weight * (1.0f - alpha), false, model);
			appendStateLayers(result, this.transition.target, alpha, false, model);
		}
		for (OverlayPlayback overlay : this.overlays)
			appendStateLayers(result, overlay.playback, overlayWeight(overlay, model), true, model);
		return List.copyOf(result);
	}

	/**
	 * Performs the cycle phase operation.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	public float cyclePhase(PBakedModel model)
	{
		Playback playback = this.transition == null ? this.current : this.transition.target;
		if (this.graph.states().get(playback.stateIndex).animationType() != PAnimationType.CYCLE)
			return Float.NaN;
		return phase(playback, model);
	}

	/**
	 * Performs the interpolated time operation.
	 * @param partialTick the partial tick to use.
	 * @return the value produced by this operation.
	 */
	public float interpolatedTime(float partialTick)
	{
		Playback playback = this.transition == null ? this.current : this.transition.target;
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		float alpha = Math.clamp(partialTick, 0.0f, 1.0f);
		return (playback.previousTime + (playback.time - playback.previousTime) * alpha) * state.speed();
	}

	/**
	 * Performs the time operation.
	 * @return the value produced by this operation.
	 */
	public float time()
	{
		return (this.transition == null ? this.current : this.transition.target).time;
	}

	/**
	 * Synchronizes the cycle.
	 * @param model the model to use.
	 * @param phase the phase to use.
	 */
	public void syncCycle(PBakedModel model, float phase)
	{
		float clampedPhase = Math.clamp(phase, 0.0f, 1.0f);
		syncPlayback(this.current, clampedPhase, model);
		if (this.transition != null)
		{
			for (WeightedPlayback source : this.transition.sources)
				syncPlayback(source.playback, clampedPhase, model);
			syncPlayback(this.transition.target, clampedPhase, model);
		}
	}

	/**
	 * Performs the trigger overlays operation.
	 */
	private void triggerOverlays()
	{
		for (int index = 0; index < this.graph.states().size(); index++)
		{
			PAnimationState state = this.graph.states().get(index);
			if (state instanceof PAnimationState.OneShotOverlay overlay && this.parameters.consumeTrigger(overlay.trigger()))
				this.overlays.add(new OverlayPlayback(new Playback(index), overlay));
		}
	}

	/**
	 * Performs the select transition operation.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	private @Nullable PAnimationTransition selectTransition(@Nullable PBakedModel model)
	{
		if (this.transition != null && this.transition.policy == PInterruptionPolicy.COMPLETE_CURRENT)
			return null;
		int source = stateIndex();
		float phase = phase(this.transition == null ? this.current : this.transition.target, model);
		return this.graph.transitions().stream().
				filter(transition -> transition.source() == source).
				filter(transition -> transition.exitTime() < 0.0f || phase >= transition.exitTime()).
				filter(transition -> transition.condition().test(this.parameters)).
				max(Comparator.comparingInt(PAnimationTransition::priority)).
				orElse(null);
	}

	/**
	 * Performs the begin transition operation.
	 * @param request the request to use.
	 * @param model the model to use.
	 */
	private void beginTransition(PAnimationTransition request, @Nullable PBakedModel model)
	{
		List<WeightedPlayback> sources;
		if (this.transition == null)
			sources = List.of(new WeightedPlayback(this.current, 1.0f));
		else if (request.interruption() == PInterruptionPolicy.FROM_CURRENT)
		{
			float alpha = this.transition.duration <= 0.0f ? 1.0f : Math.clamp(this.transition.elapsed / this.transition.duration, 0.0f, 1.0f);
			sources = new ArrayList<>(this.transition.sources.size() + 1);
			for (WeightedPlayback source : this.transition.sources)
				sources.add(new WeightedPlayback(source.playback, source.weight * (1.0f - alpha)));
			sources.add(new WeightedPlayback(this.transition.target, alpha));
		}
		else if (request.interruption() == PInterruptionPolicy.RESTART)
			sources = List.of();
		else
			return;

		Playback target = new Playback(request.target());
		PAnimationState targetState = this.graph.states().get(request.target());
		if (targetState.synchronizedCycle())
			syncPlayback(target, phase(this.transition == null ? this.current : this.transition.target, model), model);
		this.transition = new Transition(sources, target, request.blendDuration(), request.interruption());
		if (request.blendDuration() == 0.0f)
		{
			this.current = target;
			this.transition = null;
		}
	}

	/**
	 * Performs the append state layers operation.
	 * @param result the result to use.
	 * @param playback the playback to use.
	 * @param stateWeight the state weight to use.
	 * @param overlay the overlay to use.
	 * @param model the model to use.
	 */
	private void appendStateLayers(List<Layer> result, Playback playback, float stateWeight, boolean overlay, PBakedModel model)
	{
		if (stateWeight <= 0.0f)
			return;
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		for (PAnimationSample sample : state.samples(this.parameters))
			if (sample.weight() > 0.0f)
				result.add(new Layer(sample.animation(), sampleTime(playback, state, sample.animation(), model),
						state.interpolation(), stateWeight * sample.weight(), overlay));
	}

	/**
	 * Samples the time.
	 * @param playback the playback to use.
	 * @param state the state to use.
	 * @param animationName the animation name to use.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	private float sampleTime(Playback playback, PAnimationState state, String animationName, PBakedModel model)
	{
		PAnimation animation = model.animations().get(animationName);
		if (animation == null || animation.length() <= 0.0f)
			return playback.time * state.speed();
		float time = playback.time * state.speed();
		if (state.synchronizedCycle() && state.animationType() == PAnimationType.CYCLE)
			time = phase(playback, model) * animation.length();
		if (state.animationType() == PAnimationType.CYCLE)
			return time % animation.length();
		return Math.min(time, animation.length());
	}

	/**
	 * Performs the phase operation.
	 * @param playback the playback to use.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	private float phase(Playback playback, @Nullable PBakedModel model)
	{
		if (model == null)
			return 0.0f;
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		PAnimationSample sample = state.samples(this.parameters).stream().findFirst().orElse(null);
		if (sample == null)
			return 0.0f;
		PAnimation animation = model.animations().get(sample.animation());
		if (animation == null || animation.length() <= 0.0f)
			return 0.0f;
		float raw = playback.time * state.speed() / animation.length();
		return state.animationType() == PAnimationType.CYCLE ? raw - (float)Math.floor(raw) : Math.clamp(raw, 0.0f, 1.0f);
	}

	/**
	 * Synchronizes the playback.
	 * @param playback the playback to use.
	 * @param phase the phase to use.
	 * @param model the model to use.
	 */
	private void syncPlayback(Playback playback, float phase, @Nullable PBakedModel model)
	{
		if (model == null)
			return;
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		if (!state.synchronizedCycle() || state.animationType() != PAnimationType.CYCLE || state.speed() == 0.0f)
			return;
		PAnimationSample sample = state.samples(this.parameters).stream().findFirst().orElse(null);
		PAnimation animation = sample == null ? null : model.animations().get(sample.animation());
		if (animation == null || animation.length() <= 0.0f)
			return;
		playback.time = phase * animation.length() / state.speed();
		playback.previousTime = playback.time;
	}

	/**
	 * Performs the overlay finished operation.
	 * @param overlay the overlay to use.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	private boolean overlayFinished(OverlayPlayback overlay, @Nullable PBakedModel model)
	{
		if (model == null)
			return false;
		PAnimation animation = model.animations().get(overlay.overlay.animation());
		return animation != null && overlay.playback.time * overlay.overlay.speed() >= animation.length();
	}

	/**
	 * Performs the overlay weight operation.
	 * @param overlay the overlay to use.
	 * @param model the model to use.
	 * @return the value produced by this operation.
	 */
	private float overlayWeight(OverlayPlayback overlay, PBakedModel model)
	{
		PAnimation animation = model.animations().get(overlay.overlay.animation());
		if (animation == null)
			return 0.0f;
		float time = overlay.playback.time;
		float weight = overlay.overlay.fadeInDuration() <= 0.0f ? 1.0f : Math.min(time / overlay.overlay.fadeInDuration(), 1.0f);
		float remaining = animation.length() / Math.max(overlay.overlay.speed(), 1.0e-6f) - time;
		if (overlay.overlay.fadeOutDuration() > 0.0f)
			weight = Math.clamp(remaining / overlay.overlay.fadeOutDuration(), 0.0f, weight);
		return weight;
	}

	/**
	 * Performs the advance operation.
	 * @param playback the playback to use.
	 * @param delta the delta to use.
	 * @param tracks the tracks to use.
	 */
	private void advance(Playback playback, float delta, List<EventTrack> tracks)
	{
		PAnimationState state = this.graph.states().get(playback.stateIndex);
		playback.previousTime = playback.time;
		playback.time += delta;
		if (state.speed() == 0.0f)
			return;
		float from = playback.previousTime * state.speed();
		float to = playback.time * state.speed();
		for (PAnimationSample sample : state.samples(this.parameters))
			if (sample.weight() > 0.0f)
				tracks.add(new EventTrack(sample.animation(), from, to, state.animationType()));
	}

/**
 * Immutable value object representing layer.
 */
	public record Layer(String animation, float time, PInterpolationType interpolation, float weight, boolean overlay)
	{
	}
	
/**
 * Immutable value object representing event track.
 */
	public record EventTrack(String animation, float from, float to, PAnimationType animationType) { }

/**
 * Provides support for playback.
 */
	private static final class Playback
	{
		private final int stateIndex;
		private float time;
		private float previousTime;

		/**
		 * Creates an instance of the enclosing type.
		 * @param stateIndex the state index to use.
		 */
		private Playback(int stateIndex)
		{
			this.stateIndex = stateIndex;
		}
	}

/**
 * Immutable value object representing weighted playback.
 */
	private record WeightedPlayback(Playback playback, float weight)
	{
	}

/**
 * Provides support for transition.
 */
	private static final class Transition
	{
		private final List<WeightedPlayback> sources;
		private final Playback target;
		private final float duration;
		private final PInterruptionPolicy policy;
		private float elapsed;

		/**
		 * Creates an instance of the enclosing type.
		 * @param sources the sources to use.
		 * @param target the target to use.
		 * @param duration the duration to use.
		 * @param policy the policy to use.
		 */
		private Transition(List<WeightedPlayback> sources, Playback target, float duration, PInterruptionPolicy policy)
		{
			this.sources = List.copyOf(sources);
			this.target = target;
			this.duration = duration;
			this.policy = policy;
		}
	}

/**
 * Immutable value object representing overlay playback.
 */
	private record OverlayPlayback(Playback playback, PAnimationState.OneShotOverlay overlay)
	{
	}
}
