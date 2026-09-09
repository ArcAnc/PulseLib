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

public record PAnimation(String name,
                         float length,
                         Map<String, PBoneAnimation> boneAnimations,
                         List<PAnimationEvent<?>> events,
                         Map<String, PAnimationVisibilityTrack> visibilityTracks)
{
	public PAnimation(String name, float length, Map<String, PBoneAnimation> boneAnimations)
	{
		this(name, length, boneAnimations, List.of(), Map.of());
	}

	public PAnimation(String name, float length, Map<String, PBoneAnimation> boneAnimations, List<PAnimationEvent<?>> events)
	{
		this(name, length, boneAnimations, events, Map.of());
	}

	public PAnimation
	{
		events = List.copyOf(events);
		visibilityTracks = Map.copyOf(visibilityTracks);
	}

	public boolean isBoneVisible(String boneName, float time)
	{
		PAnimationVisibilityTrack track = this.visibilityTracks.get(boneName);
		return track == null || track.visibleAt(time);
	}
	
	public PRootMotionRuntime rootMotion(String rootBoneName, PInterpolationType interpolation)
	{
		return new PRootMotionRuntime(this, rootBoneName, interpolation);
	}
	
	public PRootMotionRuntime rootMotion(String rootBoneName, PInterpolationType interpolation, Object data)
	{
		return new PRootMotionRuntime(this, rootBoneName, interpolation, data);
	}

	public @Nullable BoneFrame calculateBoneTransformations(String boneName, float time, PInterpolationType interpolation)
	{
		return calculateBoneTransformations(boneName, time, interpolation, null, null);
	}

	public @Nullable BoneFrame calculateBoneTransformations(String boneName, float time, PInterpolationType interpolation, Object data)
	{
		return calculateBoneTransformations(boneName, time, interpolation, data, null);
	}

	public @Nullable BoneFrame calculateBoneTransformations(String boneName,
	                                                        float time,
	                                                        PInterpolationType interpolation,
	                                                        Object data,
	                                                        @Nullable BoneFrame accumulatedFrame)
	{
		PBoneAnimation boneAnimation = this.boneAnimations.get(boneName);
		return calculateBoneTransformations(boneAnimation, time, interpolation, data, accumulatedFrame);
	}

	public @Nullable BoneFrame calculateBoneTransformations(@Nullable PBoneAnimation boneAnimation,
	                                                        float time,
	                                                        PInterpolationType interpolation,
	                                                        Object data,
	                                                        @Nullable BoneFrame accumulatedFrame)
	{
		if (boneAnimation == null)
			return null;

		PAnimationEvaluationContext context = new PAnimationEvaluationContext(
				data instanceof MolangParser.Context molang ? molang : new MolangParser.Context(), time);
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

	public List<PAnimationEvent<?>> eventsBetween(float from, float to)
	{
		if (this.events.isEmpty() || to < from)
			return List.of();
		return this.events.stream().filter(event -> (event.time() > from || (from == 0f && event.time() == 0f)) && event.time() <= to).toList();
	}

	public List<PAnimationEvent<?>> eventsBetweenReverse(float from, float to)
	{
		if (this.events.isEmpty() || to > from)
			return List.of();
		return this.events.stream().filter(event -> event.time() >= to && event.time() < from).
			sorted(Comparator.comparingDouble((PAnimationEvent<?> event) -> event.time()).reversed()).toList();
	}

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
		List<PKeyframe<T>> keyframes = track.keyframes();
		if (keyframes.size() == 1)
			return evaluate(keyframes.getFirst().post(), channel, context);

		PKeyframe<T> previous = null;
		PKeyframe<T> next = null;
		for (PKeyframe<T> keyframe : keyframes)
		{
			if (keyframe.time() <= time)
				previous = keyframe;
			if (keyframe.time() > time)
			{
				next = keyframe;
				break;
			}
		}
		if (previous == null)
			return evaluate(keyframes.getFirst().pre(), channel, context);
		if (next == null)
			return evaluate(previous.post(), channel, context);

		T from = evaluate(previous.post(), channel, context);
		T to = evaluate(next.pre(), channel, context);
		T destination = channel.defaultValue();
		float alpha = (time - previous.time()) / (next.time() - previous.time());
		channel.interpolate(from, to, Math.clamp(alpha, 0f, 1f), previous.interpolation(), destination);
		return destination;
	}

	private static <T> T evaluate(PAnimationValue<T> value, PAnimationChannelType<T> channel, PAnimationEvaluationContext context)
	{
		T destination = channel.defaultValue();
		value.evaluate(context, destination);
		return destination;
	}
}
