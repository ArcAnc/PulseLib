/**
 * @author ArcAnc
 * Created at: 18.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.data.gecko.PMolangEulerRotationValue;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Samples sorted tracks. Catmull-Rom uses uniform point weights and normalized
 * segment time, including for uneven timestamps; it is not a time-aware spline.
 * Missing outer anchors duplicate the nearest endpoint. For segment i -> i+1,
 * controls are (i-1).post, i.post, (i+1).pre, (i+2).pre. Thus incoming/outgoing
 * values remain distinct at intentional jumps; exact key times select post.
 * Duplicate timestamps select the last key at that time; the next key is strictly
 * later, so segment duration cannot be zero. Custom two-point channels remain valid.
 */
final class PAnimationTrackSampler
{
	private PAnimationTrackSampler()
	{
	}

	/**
	 * Evaluates a channel track at the supplied animation tick position.
	 *
	 * @param <T> the sampled animation channel value type
	 * @param track the typed channel track to sample
	 * @param time the animation position in game ticks
	 * @param context the animation sampling context and reusable scratch values
	 * @return the sampled channel value, or {@code null} when the track contains no keyframes
	 */
	static <T> @Nullable T sample(PAnimationTrack<T> track, float time, PAnimationEvaluationContext context)
	{
		List<PKeyframe<T>> keys = track.keyframes();
		PAnimationChannelType<T> channel = track.channel();
		if (keys.isEmpty())
			return null;
		if (keys.size() == 1)
			return evaluate(keys.getFirst().post(), channel, context);
		int index = -1;
		while (index + 1 < keys.size() && keys.get(index + 1).time() <= time)
			index++;
		if (index < 0)
			return evaluate(keys.getFirst().pre(), channel, context);
		PKeyframe<T> left = keys.get(index);
		if (index + 1 == keys.size())
			return evaluate(left.post(), channel, context);
		PKeyframe<T> right = keys.get(index + 1);
		float alpha = Math.clamp((time - left.time()) / (right.time() - left.time()), 0f, 1f);
		if (left.post() instanceof PMolangEulerRotationValue fromEuler &&
				right.pre() instanceof PMolangEulerRotationValue toEuler && channel.valueClass() == Quaternionf.class)
			return channel.valueClass().cast(sampleEuler(keys, index, fromEuler, toEuler, alpha, context));
		T from = evaluate(left.post(), channel, context);
		T to = evaluate(right.pre(), channel, context);
		T destination = channel.defaultValue();
		if (left.interpolation() == PInterpolationType.CATMULLROM)
		{
			T before = index > 0 ? evaluate(keys.get(index - 1).post(), channel, context) : from;
			T after = index + 2 < keys.size() ? evaluate(keys.get(index + 2).pre(), channel, context) : to;
			channel.interpolate(before, from, to, after, alpha, left.interpolation(), destination);
		}
		else
			channel.interpolate(from, to, alpha, left.interpolation(), destination);
		return destination;
	}

	private static Quaternionf sampleEuler(List<? extends PKeyframe<?>> keys, int index,
	                                       PMolangEulerRotationValue fromValue, PMolangEulerRotationValue toValue,
	                                       float alpha, PAnimationEvaluationContext context)
	{
		Vector3f from = new Vector3f();
		Vector3f to = new Vector3f();
		fromValue.evaluateEuler(context, from);
		toValue.evaluateEuler(context, to);
		PInterpolation interpolation = keys.get(index).interpolation();
		Vector3f result = new Vector3f();
		if (interpolation == PInterpolationType.CATMULLROM)
		{
			Vector3f before = from;
			Vector3f after = to;
			if (index > 0 && keys.get(index - 1).post() instanceof PMolangEulerRotationValue value)
			{
				before = new Vector3f();
				value.evaluateEuler(context, before);
			}
			if (index + 2 < keys.size() && keys.get(index + 2).pre() instanceof PMolangEulerRotationValue value)
			{
				after = new Vector3f();
				value.evaluateEuler(context, after);
			}
			result.set(
					(float)PInterpolationType.catmullRom(alpha, before.x, from.x, to.x, after.x),
					(float)PInterpolationType.catmullRom(alpha, before.y, from.y, to.y, after.y),
					(float)PInterpolationType.catmullRom(alpha, before.z, from.z, to.z, after.z));
		}
		else
			result.set(from).lerp(to, (float)interpolation.buildTransformer(alpha));
		return new Quaternionf().rotationXYZ((float)Math.toRadians(result.x),
				(float)Math.toRadians(result.y), (float)Math.toRadians(result.z));
	}

	private static <T> T evaluate(PAnimationValue<T> value, PAnimationChannelType<T> channel, PAnimationEvaluationContext context)
	{
		T destination = channel.defaultValue();
		value.evaluate(context, destination);
		return destination;
	}
}
