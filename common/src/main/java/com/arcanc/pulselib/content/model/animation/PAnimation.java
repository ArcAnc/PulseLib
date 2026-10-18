/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.data.gecko.MolangParser;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Animation clip containing per-bone tracks, timed events and visibility tracks. Playback positions and
 * clip length are measured in game ticks.
 *
 * @param name the animation clip name
 * @param length the animation clip length in game ticks
 * @param boneAnimations the animation tracks associated with model bones
 * @param events the events scheduled on this animation timeline
 * @param visibilityTracks the step-sampled visibility tracks indexed by bone name
 */
public record PAnimation(String name,
                         float length,
                         Map<String, PBoneAnimation> boneAnimations,
                         List<PAnimationEvent<?>> events,
                         Map<String, PAnimationVisibilityTrack> visibilityTracks)
{
	/**
	 * Creates an animation clip with bone tracks and any supplied event and visibility timelines.
	 *
	 * @param name the animation clip name
	 * @param length the animation clip length in game ticks
	 * @param boneAnimations the animation tracks associated with model bones
	 */
	public PAnimation(String name, float length, Map<String, PBoneAnimation> boneAnimations)
	{
		this(name, length, boneAnimations, List.of(), Map.of());
	}

	/**
	 * Creates an animation clip with bone tracks and any supplied event and visibility timelines.
	 *
	 * @param name the animation clip name
	 * @param length the animation clip length in game ticks
	 * @param boneAnimations the animation tracks associated with model bones
	 * @param events the events scheduled on this animation timeline
	 */
	public PAnimation(String name, float length, Map<String, PBoneAnimation> boneAnimations, List<PAnimationEvent<?>> events)
	{
		this(name, length, boneAnimations, events, Map.of());
	}

	/**
	 * Creates an animation clip with bone tracks and any supplied event and visibility timelines.
	 *
	 * @param name the animation clip name
	 * @param length the animation clip length in game ticks
	 * @param boneAnimations the animation tracks associated with model bones
	 * @param events the events scheduled on this animation timeline
	 * @param visibilityTracks the step-sampled visibility tracks indexed by bone name
	 */
	public PAnimation
	{
		events = List.copyOf(events);
		visibilityTracks = Map.copyOf(visibilityTracks);
	}

	/**
	 * Samples the named bone visibility track, treating bones without a track as visible.
	 *
	 * @param boneName the animation bone name
	 * @param time the animation position in game ticks
	 * @return whether the bone is visible at this animation position
	 */
	public boolean isBoneVisible(String boneName, float time)
	{
		PAnimationVisibilityTrack track = this.visibilityTracks.get(boneName);
		return track == null || track.visibleAt(time);
	}
	
	/**
	 * Creates a root-motion sampler for the named root bone.
	 *
	 * @param rootBoneName the bone used as the root-motion or relative-transform reference
	 * @param interpolation the keyframe interpolation policy
	 * @return the root-motion sampler for this clip and root bone
	 */
	public PRootMotionRuntime rootMotion(String rootBoneName, PInterpolationType interpolation)
	{
		return new PRootMotionRuntime(this, rootBoneName, interpolation);
	}
	
	/**
	 * Creates a root-motion sampler for the named root bone.
	 *
	 * @param rootBoneName the bone used as the root-motion or relative-transform reference
	 * @param interpolation the keyframe interpolation policy
	 * @param data the optional expression context used to sample source-format animation values
	 * @return the root-motion sampler for this clip and root bone
	 */
	public PRootMotionRuntime rootMotion(String rootBoneName, PInterpolationType interpolation, Object data)
	{
		return new PRootMotionRuntime(this, rootBoneName, interpolation, data);
	}

	/**
	 * Samples translation, rotation and scale tracks for one bone at the supplied tick position.
	 *
	 * @param boneName the animation bone name
	 * @param time the animation position in game ticks
	 * @param interpolation the keyframe interpolation policy
	 * @return the sampled local transform of the selected bone, or {@code null} when the selected bone has no animation tracks
	 */
	public @Nullable BoneFrame calculateBoneTransformations(String boneName, float time, PInterpolationType interpolation)
	{
		return calculateBoneTransformations(boneName, time, interpolation, null, null);
	}

	/**
	 * Samples translation, rotation and scale tracks for one bone at the supplied tick position.
	 *
	 * @param boneName the animation bone name
	 * @param time the animation position in game ticks
	 * @param interpolation the keyframe interpolation policy
	 * @param data the optional expression context used to sample source-format animation values
	 * @return the sampled local transform of the selected bone, or {@code null} when the selected bone has no animation tracks
	 */
	public @Nullable BoneFrame calculateBoneTransformations(String boneName, float time, PInterpolationType interpolation, Object data)
	{
		return calculateBoneTransformations(boneName, time, interpolation, data, null);
	}

	/**
	 * Samples translation, rotation and scale tracks for one bone at the supplied tick position.
	 *
	 * @param boneName the animation bone name
	 * @param time the animation position in game ticks
	 * @param interpolation the keyframe interpolation policy
	 * @param data the optional expression context used to sample source-format animation values
	 * @param accumulatedFrame the transform contributed by preceding controllers; may be {@code null}
	 * @return the sampled local transform of the selected bone, or {@code null} when the selected bone has no animation tracks
	 */
	public @Nullable BoneFrame calculateBoneTransformations(String boneName,
	                                                        float time,
	                                                        PInterpolationType interpolation,
	                                                        Object data,
	                                                        @Nullable BoneFrame accumulatedFrame)
	{
		PBoneAnimation boneAnimation = this.boneAnimations.get(boneName);
		return calculateBoneTransformations(boneAnimation, time, interpolation, data, accumulatedFrame);
	}

	/**
	 * Samples translation, rotation and scale tracks for one bone at the supplied tick position.
	 *
	 * @param boneAnimation the bone animation; may be {@code null}
	 * @param time the animation position in game ticks
	 * @param interpolation the keyframe interpolation policy
	 * @param data the optional expression context used to sample source-format animation values
	 * @param accumulatedFrame the transform contributed by preceding controllers; may be {@code null}
	 * @return the sampled local transform of the selected bone, or {@code null} when the selected bone has no animation tracks
	 */
	public @Nullable BoneFrame calculateBoneTransformations(@Nullable PBoneAnimation boneAnimation,
	                                                        float time,
	                                                        PInterpolationType interpolation,
	                                                        Object data,
	                                                        @Nullable BoneFrame accumulatedFrame)
	{
		if (boneAnimation == null)
			return null;

		PAnimationEvaluationContext context = new PAnimationEvaluationContext(
				data instanceof MolangParser.Context molang ? molang : new MolangParser.Context().
						query("anim_time", PAnimationEvaluationContext.ticksToSeconds(time)), time);
		Vector3f accumulatedTranslation = accumulatedFrame == null ? new Vector3f() : accumulatedFrame.translation();
		Vector3f accumulatedScale = accumulatedFrame == null ? new Vector3f(1f) : accumulatedFrame.scale();
		Vector3f accumulatedRotation = accumulatedFrame == null ? new Vector3f() :
				accumulatedFrame.rotation().getEulerAnglesXYZ(new Vector3f()).mul((float) (180d / Math.PI));

		Vector3f translation = sample(boneAnimation, PLibRegistration.AnimationChannelReg.POSITION, time, context, accumulatedTranslation);
		Vector3f scale = sample(boneAnimation, PLibRegistration.AnimationChannelReg.SCALE, time, context, accumulatedScale);
		Quaternionf rotation = sample(boneAnimation, PLibRegistration.AnimationChannelReg.ROTATION, time, context, accumulatedRotation);
		if (translation == null && scale == null && rotation == null)
			return null;
		return new BoneFrame(
				translation == null ? new Vector3f() : translation,
				rotation == null ? new Quaternionf() : rotation,
				scale == null ? new Vector3f(1f) : scale);
	}

	/**
	 * Visits all crossed cyclic events, including distinct zero/end markers.
	 * Empty and zero-length clips do not iterate. Pathological ranges fail with
	 * a diagnostic before dispatch rather than silently dropping events.
	 * @param from the previous playback time, in ticks.
	 * @param to the next playback time, in ticks.
	 * @param consumer the event receiver.
	 */
	public void forEachCyclicEvent(double from, double to, Consumer<PAnimationEvent<?>> consumer)
	{
		forEachCyclicEvent(from, to, true, consumer);
	}

	/**
	 * Visits a cyclic window, optionally excluding the initial reverse end marker.
	 * Raw reverse playback excludes that marker on stage entry; subsequent wraps
	 * include it. All later windows retain the ordinary seam semantics.
	 * @param from the previous playback time, in ticks.
	 * @param to the next playback time, in ticks.
	 * @param includeReverseStart whether to include an end marker on departure.
	 * @param consumer the event receiver.
	 */
	public void forEachCyclicEvent(double from, double to, boolean includeReverseStart, Consumer<PAnimationEvent<?>> consumer)
	{
		PCyclicAnimationEvents.visit(this, from, to, includeReverseStart, consumer);
	}

	/**
	 * Collects events crossed by a forward playback interval.
	 *
	 * @param from the preceding animation tick position
	 * @param to the current animation tick position
	 * @return the events crossed in forward playback order
	 */
	public List<PAnimationEvent<?>> eventsBetween(float from, float to)
	{
		if (this.events.isEmpty() || to < from)
			return List.of();
		return this.events.stream().filter(event -> (event.time() > from || (from == 0f && event.time() == 0f)) && event.time() <= to).toList();
	}

	/**
	 * Collects events crossed by a reverse playback interval.
	 *
	 * @param from the preceding animation tick position
	 * @param to the current animation tick position
	 * @return the events crossed in reverse playback order
	 */
	public List<PAnimationEvent<?>> eventsBetweenReverse(float from, float to)
	{
		if (this.events.isEmpty() || to > from)
			return List.of();
		return this.events.stream().filter(event -> event.time() >= to && event.time() < from).
			sorted(Comparator.comparingDouble((PAnimationEvent<?> event) -> event.time()).reversed()).toList();
	}

	/**
	 * Samples the selected typed bone track with the current evaluation and accumulated component values.
	 *
	 * @param <T> the value type carried by this contract
	 * @param boneAnimation the bone animation
	 * @param channel the typed channel controlling interpolation and pose application
	 * @param time the animation position in game ticks
	 * @param context the animation sampling context and reusable scratch values
	 * @param thisValue the this value
	 * @return the sampled selected typed bone track with the current evaluation and accumulated component values, or {@code null} when the selected bone has no animation tracks
	 */
	private static <T> @Nullable T sample(PBoneAnimation boneAnimation,
	                                      PAnimationChannelType<T> channel,
	                                      float time,
	                                      PAnimationEvaluationContext context,
	                                      Vector3f thisValue)
	{
		PAnimationTrack<T> track = boneAnimation.track(channel);
		if (track == null || track.keyframes().isEmpty())
			return null;
		context.thisValues(thisValue);
		return PAnimationTrackSampler.sample(track, time, context);
	}
}
