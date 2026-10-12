/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class PPoseMixer
{
	private static final float EPSILON = 1.0e-6f;

	private final int boneCount;
	private final int[] parents;

	public PPoseMixer(int boneCount)
	{
		this(boneCount, null);
	}

	public PPoseMixer(int boneCount, @Nullable int[] parents)
	{
		if (boneCount < 0)
			throw new IllegalArgumentException("Bone count must be non-negative");
		if (parents != null && parents.length != boneCount)
			throw new IllegalArgumentException("Parent hierarchy must have one entry per bone");
		this.boneCount = boneCount;
		this.parents = parents == null ? null : parents.clone();
	}

	public PPose mix(PPose referencePose, Collection<Layer> layers)
	{
		Objects.requireNonNull(referencePose);
		if (referencePose.boneCount() != this.boneCount)
			throw new IllegalArgumentException("Reference pose does not belong to this mixer");

		PPose result = copy(referencePose);
		List<Layer> ordered = new ArrayList<>(layers);
		ordered.removeIf(layer -> layer.weight() <= 0.0f);
		ordered.sort(Comparator.comparingInt(Layer :: priority));

		for (int offset = 0; offset < ordered.size(); )
		{
			int priority = ordered.get(offset).priority();
			int end = offset + 1;
			while (end < ordered.size() && ordered.get(end).priority() == priority)
				end++;
			mixPriority(result, referencePose, ordered.subList(offset, end));
			offset = end;
		}
		markDirty(referencePose, result);
		return result;
	}

	/** Creates the two weighted layers forming a crossfade at {@code elapsed}. */
	public static List<Layer> crossfade(Layer outgoing,
	                                    Layer incoming,
	                                    float elapsed,
	                                    float duration,
	                                    PPoseEasing easing)
	{
		Objects.requireNonNull(outgoing);
		Objects.requireNonNull(incoming);
		Objects.requireNonNull(easing);
		float alpha = duration <= 0.0f ? 1.0f : easing.transform(elapsed / duration);
		return List.of(outgoing.withWeight(outgoing.weight() * (1.0f - alpha)), incoming.withWeight(incoming.weight() * alpha));
	}

	/** Stateful crossfade helper with explicit interruption semantics. */
	public static final class Transition
	{
		private List<Layer> sourceLayers;
		private Layer target;
		private float elapsed;
		private float duration;
		private PPoseEasing easing;

		public Transition(Layer initial)
		{
			this.sourceLayers = List.of(Objects.requireNonNull(initial));
			this.target = initial;
			this.elapsed = 1.0f;
			this.duration = 0.0f;
			this.easing = PPoseEasing.LINEAR;
		}

		public boolean transitionTo(Layer target,
		                            float duration,
		                            PPoseEasing easing,
		                            PTransitionInterruptionPolicy interruptionPolicy)
		{
			Objects.requireNonNull(target);
			Objects.requireNonNull(easing);
			Objects.requireNonNull(interruptionPolicy);
			if (isTransitioning() && interruptionPolicy == PTransitionInterruptionPolicy.COMPLETE_CURRENT)
				return false;
			if (isTransitioning() && interruptionPolicy == PTransitionInterruptionPolicy.FROM_CURRENT)
				this.sourceLayers = List.copyOf(layers());
			else if (interruptionPolicy == PTransitionInterruptionPolicy.RESTART)
				this.sourceLayers = List.of();
			else if (!isTransitioning())
				this.sourceLayers = List.of(this.target);
			this.target = target;
			this.elapsed = 0.0f;
			this.duration = Math.max(duration, 0.0f);
			this.easing = easing;
			return true;
		}

		public void advance(float delta)
		{
			this.elapsed = Math.min(this.elapsed + Math.max(delta, 0.0f), this.duration);
		}

		public boolean isTransitioning()
		{
			return this.elapsed < this.duration;
		}

		public List<Layer> layers()
		{
			if (!isTransitioning())
				return List.of(this.target);
			float alpha = this.duration <= 0.0f ? 1.0f : this.easing.transform(this.elapsed / this.duration);
			List<Layer> layers = new ArrayList<>(this.sourceLayers.size() + 1);
			for (Layer source : this.sourceLayers)
				layers.add(source.withWeight(source.weight() * (1.0f - alpha)));
			layers.add(this.target.withWeight(this.target.weight() * alpha));
			return List.copyOf(layers);
		}
	}

	private void mixPriority(PPose result, PPose reference, List<Layer> layers)
	{
		for (int bone = 0; bone < this.boneCount; bone++)
		{
			mixOverrides(result, layers, bone);
			for (Layer layer : layers)
			{
				if (layer.mode() == PPoseBlendMode.OVERRIDE)
					continue;
				float weight = layer.boneWeight(bone);
				if (weight <= 0.0f)
					continue;
				apply(result, reference, layer, bone, weight);
			}
		}
	}

	private void mixOverrides(PPose result, List<Layer> layers, int bone)
	{
		float totalWeight = 0.0f;
		Vector3f translation = new Vector3f();
		Vector3f scale = new Vector3f();
		Quaternionf anchor = null;
		float qx = 0.0f;
		float qy = 0.0f;
		float qz = 0.0f;
		float qw = 0.0f;
		for (Layer layer : layers)
		{
			if (layer.mode() != PPoseBlendMode.OVERRIDE)
				continue;
			float weight = layer.boneWeight(bone);
			if (weight <= 0.0f)
				continue;
			PPose pose = layer.pose();
			translation.fma(weight, pose.translation(bone));
			scale.fma(weight, pose.scale(bone));
			Quaternionf rotation = pose.rotation(bone);
			if (anchor == null)
				anchor = rotation;
			float sign = anchor.dot(rotation) < 0.0f ? -1.0f : 1.0f;
			qx += rotation.x * weight * sign;
			qy += rotation.y * weight * sign;
			qz += rotation.z * weight * sign;
			qw += rotation.w * weight * sign;
			totalWeight += weight;
		}
		if (totalWeight <= 0.0f)
			return;
		float inverseWeight = 1.0f / totalWeight;
		translation.mul(inverseWeight);
		scale.mul(inverseWeight);
		Quaternionf rotation = new Quaternionf(qx * inverseWeight, qy * inverseWeight, qz * inverseWeight, qw * inverseWeight).normalize();
		float alpha = Math.min(totalWeight, 1.0f);
		result.translation(bone).lerp(translation, alpha);
		result.scale(bone).lerp(scale, alpha);
		shortestSlerp(result.rotation(bone), rotation, alpha);
	}

	private void apply(PPose result, PPose reference, Layer layer, int bone, float weight)
	{
		PPose pose = layer.pose();
		PPose layerReference = layer.referencePose() == null ? reference : layer.referencePose();
		Vector3f translationDelta = new Vector3f(pose.translation(bone)).sub(layerReference.translation(bone));
		Quaternionf rotationDelta = new Quaternionf(layerReference.rotation(bone)).invert().premul(pose.rotation(bone)).normalize();
		Vector3f scaleFactor = divide(pose.scale(bone), layerReference.scale(bone));
		switch (layer.mode())
		{
			case ADDITIVE_LOCAL -> addLocal(result, bone, translationDelta, rotationDelta, scaleFactor, weight, false);
			case ADDITIVE_MESH_SPACE -> addMeshSpace(result, bone, translationDelta, rotationDelta, scaleFactor, weight);
			case MULTIPLY_SCALE -> result.scale(bone).mul(weightedScale(scaleFactor, weight));
			case DIFFERENCE -> addLocal(result, bone, translationDelta, rotationDelta, scaleFactor, weight, true);
			case OVERRIDE -> throw new IllegalStateException("Override layers are handled as a group");
		}
	}

	private void addLocal(PPose result,
	                      int bone,
	                      Vector3f translation,
	                      Quaternionf rotation,
	                      Vector3f scale,
	                      float weight,
	                      boolean inverse)
	{
		float direction = inverse ? -weight : weight;
		result.translation(bone).fma(direction, translation);
		Quaternionf delta = weightedRotation(rotation, weight);
		if (inverse)
			delta.invert();
		result.rotation(bone).premul(delta).normalize();
		Vector3f factor = weightedScale(scale, weight);
		if (inverse)
			factor.set(safeInverse(factor.x), safeInverse(factor.y), safeInverse(factor.z));
		result.scale(bone).mul(factor);
	}

	private void addMeshSpace(PPose result,
	                          int bone,
	                          Vector3f translation,
	                          Quaternionf rotation,
	                          Vector3f scale,
	                          float weight)
	{
		Quaternionf parentRotation = parentModelRotation(result, bone);
		Vector3f localTranslation = parentRotation.transformInverse(new Vector3f(translation));
		Quaternionf localRotation = new Quaternionf(parentRotation).invert().premul(rotation).mul(parentRotation).normalize();
		addLocal(result, bone, localTranslation, localRotation, scale, weight, false);
	}

	private Quaternionf parentModelRotation(PPose pose, int bone)
	{
		Quaternionf result = new Quaternionf();
		if (this.parents == null)
			return result;
		for (int parent = this.parents[bone]; parent >= 0; parent = this.parents[parent])
			result.premul(pose.rotation(parent));
		return result.normalize();
	}

	private static PPose copy(PPose source)
	{
		PPose copy = new PPose(source.boneCount());
		for (int bone = 0; bone < source.boneCount(); bone++)
			copy.set(bone, source.translation(bone), source.rotation(bone), source.scale(bone));
		return copy;
	}

	private static void markDirty(PPose reference, PPose result)
	{
		for (int bone = 0; bone < result.boneCount(); bone++)
			if (result.translation(bone).distanceSquared(reference.translation(bone)) > EPSILON * EPSILON ||
					Math.abs(Math.abs(result.rotation(bone).dot(reference.rotation(bone))) - 1.0f) > EPSILON ||
					result.scale(bone).distanceSquared(reference.scale(bone)) > EPSILON * EPSILON)
				result.setAnimated(bone, result.translation(bone), result.rotation(bone), result.scale(bone));
	}

	private static void shortestSlerp(Quaternionf destination, Quaternionf target, float alpha)
	{
		Quaternionf shortestTarget = new Quaternionf(target);
		if (destination.dot(shortestTarget) < 0.0f)
			shortestTarget.set(-shortestTarget.x, -shortestTarget.y, -shortestTarget.z, -shortestTarget.w);
		destination.slerp(shortestTarget, alpha).normalize();
	}

	private static Quaternionf weightedRotation(Quaternionf rotation, float weight)
	{
		Quaternionf shortestRotation = new Quaternionf(rotation);
		if (shortestRotation.w < 0.0f)
			shortestRotation.set(-shortestRotation.x, -shortestRotation.y, -shortestRotation.z, -shortestRotation.w);
		return new Quaternionf().slerp(shortestRotation, Math.clamp(weight, 0.0f, 1.0f)).normalize();
	}

	private static Vector3f weightedScale(Vector3f scale, float weight)
	{
		return new Vector3f(1.0f).lerp(scale, Math.clamp(weight, 0.0f, 1.0f));
	}

	private static Vector3f divide(Vector3f value, Vector3f divisor)
	{
		return new Vector3f(value.x * safeInverse(divisor.x), value.y * safeInverse(divisor.y), value.z * safeInverse(divisor.z));
	}

	private static float safeInverse(float value)
	{
		return Math.abs(value) < EPSILON ? 1.0f : 1.0f / value;
	}

	@FunctionalInterface
	public interface BoneWeight
	{
		BoneWeight FULL = boneIndex -> 1.0f;

		float weight(int boneIndex);
	}

	public record Layer(PPose pose,
	                    @Nullable PPose referencePose,
	                    BitSet mask,
	                    BoneWeight boneWeight,
	                    PPoseBlendMode mode,
	                    int priority,
	                    float weight)
	{
		public Layer
		{
			pose = Objects.requireNonNull(pose);
			mask = mask == null ? allBones(pose.boneCount()) : (BitSet) mask.clone();
			boneWeight = boneWeight == null ? BoneWeight.FULL : boneWeight;
			mode = Objects.requireNonNull(mode);
			weight = Math.clamp(weight, 0.0f, 1.0f);
			if (referencePose != null && referencePose.boneCount() != pose.boneCount())
				throw new IllegalArgumentException("Layer and reference poses must have equal bone counts");
		}

		public Layer(PPose pose, PPoseBlendMode mode, int priority, float weight)
		{
			this(pose, null, null, null, mode, priority, weight);
		}

		public float boneWeight(int boneIndex)
		{
			return this.mask.get(boneIndex) ? Math.clamp(this.weight * this.boneWeight.weight(boneIndex), 0.0f, 1.0f) : 0.0f;
		}

		public Layer withWeight(float weight)
		{
			return new Layer(this.pose, this.referencePose, this.mask, this.boneWeight, this.mode, this.priority, weight);
		}

		private static BitSet allBones(int boneCount)
		{
			BitSet result = new BitSet(boneCount);
			result.set(0, boneCount);
			return result;
		}
	}
}
