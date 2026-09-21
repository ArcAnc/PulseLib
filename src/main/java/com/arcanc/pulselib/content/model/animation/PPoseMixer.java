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

/**
 * Provides support for pose mixer.
 */
public final class PPoseMixer
{
	private static final float EPSILON = 1.0e-6f;

	private final int boneCount;
	private final int[] parents;

	/**
	 * Creates an instance of the enclosing type.
	 * @param boneCount the bone count to use.
	 */
	public PPoseMixer(int boneCount)
	{
		this(boneCount, null);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param boneCount the bone count to use.
	 * @param parents the parents to use.
	 */
	public PPoseMixer(int boneCount, @Nullable int[] parents)
	{
		if (boneCount < 0)
			throw new IllegalArgumentException("Bone count must be non-negative");
		if (parents != null && parents.length != boneCount)
			throw new IllegalArgumentException("Parent hierarchy must have one entry per bone");
		this.boneCount = boneCount;
		this.parents = parents == null ? null : parents.clone();
	}

	/**
	 * Performs the mix operation.
	 * @param referencePose the reference pose to use.
	 * @param layers the layers to use.
	 * @return the value produced by this operation.
	 */
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

		/**
		 * Creates an instance of the enclosing type.
		 * @param initial the initial to use.
		 */
		public Transition(Layer initial)
		{
			this.sourceLayers = List.of(Objects.requireNonNull(initial));
			this.target = initial;
			this.elapsed = 1.0f;
			this.duration = 0.0f;
			this.easing = PPoseEasing.LINEAR;
		}

		/**
		 * Performs the transition to operation.
		 * @param target the target to use.
		 * @param duration the duration to use.
		 * @param easing the easing to use.
		 * @param interruptionPolicy the interruption policy to use.
		 * @return the value produced by this operation.
		 */
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

		/**
		 * Performs the advance operation.
		 * @param delta the delta to use.
		 */
		public void advance(float delta)
		{
			this.elapsed = Math.min(this.elapsed + Math.max(delta, 0.0f), this.duration);
		}

		/**
		 * Determines whether transitioning.
		 * @return the value produced by this operation.
		 */
		public boolean isTransitioning()
		{
			return this.elapsed < this.duration;
		}

		/**
		 * Performs the layers operation.
		 * @return the value produced by this operation.
		 */
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

	/**
	 * Performs the mix priority operation.
	 * @param result the result to use.
	 * @param reference the reference to use.
	 * @param layers the layers to use.
	 */
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

	/**
	 * Performs the mix overrides operation.
	 * @param result the result to use.
	 * @param layers the layers to use.
	 * @param bone the bone to use.
	 */
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

	/**
	 * Performs the apply operation.
	 * @param result the result to use.
	 * @param reference the reference to use.
	 * @param layer the layer to use.
	 * @param bone the bone to use.
	 * @param weight the weight to use.
	 */
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

	/**
	 * Adds the local.
	 * @param result the result to use.
	 * @param bone the bone to use.
	 * @param translation the translation to use.
	 * @param rotation the rotation to use.
	 * @param scale the scale to use.
	 * @param weight the weight to use.
	 * @param inverse the inverse to use.
	 */
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

	/**
	 * Adds the mesh space.
	 * @param result the result to use.
	 * @param bone the bone to use.
	 * @param translation the translation to use.
	 * @param rotation the rotation to use.
	 * @param scale the scale to use.
	 * @param weight the weight to use.
	 */
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

	/**
	 * Performs the parent model rotation operation.
	 * @param pose the pose to use.
	 * @param bone the bone to use.
	 * @return the value produced by this operation.
	 */
	private Quaternionf parentModelRotation(PPose pose, int bone)
	{
		Quaternionf result = new Quaternionf();
		if (this.parents == null)
			return result;
		for (int parent = this.parents[bone]; parent >= 0; parent = this.parents[parent])
			result.premul(pose.rotation(parent));
		return result.normalize();
	}

	/**
	 * Performs the copy operation.
	 * @param source the source to use.
	 * @return the value produced by this operation.
	 */
	private static PPose copy(PPose source)
	{
		PPose copy = new PPose(source.boneCount());
		for (int bone = 0; bone < source.boneCount(); bone++)
			copy.set(bone, source.translation(bone), source.rotation(bone), source.scale(bone));
		return copy;
	}

	/**
	 * Performs the mark dirty operation.
	 * @param reference the reference to use.
	 * @param result the result to use.
	 */
	private static void markDirty(PPose reference, PPose result)
	{
		for (int bone = 0; bone < result.boneCount(); bone++)
			if (result.translation(bone).distanceSquared(reference.translation(bone)) > EPSILON * EPSILON ||
					Math.abs(Math.abs(result.rotation(bone).dot(reference.rotation(bone))) - 1.0f) > EPSILON ||
					result.scale(bone).distanceSquared(reference.scale(bone)) > EPSILON * EPSILON)
				result.setAnimated(bone, result.translation(bone), result.rotation(bone), result.scale(bone));
	}

	/**
	 * Performs the shortest slerp operation.
	 * @param destination the destination to use.
	 * @param target the target to use.
	 * @param alpha the alpha to use.
	 */
	private static void shortestSlerp(Quaternionf destination, Quaternionf target, float alpha)
	{
		Quaternionf shortestTarget = new Quaternionf(target);
		if (destination.dot(shortestTarget) < 0.0f)
			shortestTarget.set(-shortestTarget.x, -shortestTarget.y, -shortestTarget.z, -shortestTarget.w);
		destination.slerp(shortestTarget, alpha).normalize();
	}

	/**
	 * Performs the weighted rotation operation.
	 * @param rotation the rotation to use.
	 * @param weight the weight to use.
	 * @return the value produced by this operation.
	 */
	private static Quaternionf weightedRotation(Quaternionf rotation, float weight)
	{
		Quaternionf shortestRotation = new Quaternionf(rotation);
		if (shortestRotation.w < 0.0f)
			shortestRotation.set(-shortestRotation.x, -shortestRotation.y, -shortestRotation.z, -shortestRotation.w);
		return new Quaternionf().slerp(shortestRotation, Math.clamp(weight, 0.0f, 1.0f)).normalize();
	}

	/**
	 * Performs the weighted scale operation.
	 * @param scale the scale to use.
	 * @param weight the weight to use.
	 * @return the value produced by this operation.
	 */
	private static Vector3f weightedScale(Vector3f scale, float weight)
	{
		return new Vector3f(1.0f).lerp(scale, Math.clamp(weight, 0.0f, 1.0f));
	}

	/**
	 * Performs the divide operation.
	 * @param value the value to use.
	 * @param divisor the divisor to use.
	 * @return the value produced by this operation.
	 */
	private static Vector3f divide(Vector3f value, Vector3f divisor)
	{
		return new Vector3f(value.x * safeInverse(divisor.x), value.y * safeInverse(divisor.y), value.z * safeInverse(divisor.z));
	}

	/**
	 * Performs the safe inverse operation.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	private static float safeInverse(float value)
	{
		return Math.abs(value) < EPSILON ? 1.0f : 1.0f / value;
	}

	@FunctionalInterface
/**
 * Defines the contract for bone weight.
 */
	public interface BoneWeight
	{
		BoneWeight FULL = boneIndex -> 1.0f;

		/**
		 * Performs the weight operation.
		 * @param boneIndex the bone index to use.
		 * @return the value produced by this operation.
		 */
		float weight(int boneIndex);
	}

/**
 * Immutable value object representing layer.
 */
	public record Layer(PPose pose,
	                    @Nullable PPose referencePose,
	                    BitSet mask,
	                    BoneWeight boneWeight,
	                    PPoseBlendMode mode,
	                    int priority,
	                    float weight)
	{
		/**
		 * Creates an instance of the enclosing type.
		 * @param pose the pose to use.
		 * @param referencePose the reference pose to use.
		 * @param mask the mask to use.
		 * @param boneWeight the bone weight to use.
		 * @param mode the mode to use.
		 * @param priority the priority to use.
		 * @param weight the weight to use.
		 */
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

		/**
		 * Creates an instance of the enclosing type.
		 * @param pose the pose to use.
		 * @param mode the mode to use.
		 * @param priority the priority to use.
		 * @param weight the weight to use.
		 */
		public Layer(PPose pose, PPoseBlendMode mode, int priority, float weight)
		{
			this(pose, null, null, null, mode, priority, weight);
		}

		/**
		 * Performs the bone weight operation.
		 * @param boneIndex the bone index to use.
		 * @return the value produced by this operation.
		 */
		public float boneWeight(int boneIndex)
		{
			return this.mask.get(boneIndex) ? Math.clamp(this.weight * this.boneWeight.weight(boneIndex), 0.0f, 1.0f) : 0.0f;
		}

		/**
		 * Performs the with weight operation.
		 * @param weight the weight to use.
		 * @return the value produced by this operation.
		 */
		public Layer withWeight(float weight)
		{
			return new Layer(this.pose, this.referencePose, this.mask, this.boneWeight, this.mode, this.priority, weight);
		}

		/**
		 * Performs the all bones operation.
		 * @param boneCount the bone count to use.
		 * @return the value produced by this operation.
		 */
		private static BitSet allBones(int boneCount)
		{
			BitSet result = new BitSet(boneCount);
			result.set(0, boneCount);
			return result;
		}
	}
}
