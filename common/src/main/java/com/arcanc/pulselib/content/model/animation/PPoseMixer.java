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
import java.util.function.Supplier;

/**
 * Combines prioritized pose layers with masks, bone weights and configurable transform-space blend modes.
 */
public final class PPoseMixer
{
	private static final float EPSILON = 1.0e-6f;

	private final int boneCount;
	private final int @Nullable [] parents;

	/**
	 * Creates a pose mixer for the requested bone count and optional parent hierarchy.
	 *
	 * @param boneCount the number of indexed bones
	 */
	public PPoseMixer(int boneCount)
	{
		this(boneCount, null);
	}

	/**
	 * Creates a pose mixer for the requested bone count and optional parent hierarchy.
	 *
	 * @param boneCount the number of indexed bones
	 * @param parents the parent bone indices, with a root sentinel for root bones; may be {@code null}
	 */
	public PPoseMixer(int boneCount, int @Nullable [] parents)
	{
		if (boneCount < 0)
		{
			throw new IllegalArgumentException("Bone count must be non-negative");
		}
		if (parents != null && parents.length != boneCount)
		{
			throw new IllegalArgumentException("Parent hierarchy must have one entry per bone");
		}
		this.boneCount = boneCount;
		this.parents = parents == null ? null : parents.clone();
	}

	/**
	 * Combines the supplied layers over the reference pose in priority order.
	 *
	 * @param referencePose the reference pose used to calculate layer deltas
	 * @param layers the layers
	 * @return the indexed bone pose
	 */
	public PPose mix(PPose referencePose, Collection<Layer> layers)
	{
		Objects.requireNonNull(referencePose);
		if (referencePose.boneCount() != this.boneCount)
		{
			throw new IllegalArgumentException("Reference pose does not belong to this mixer");
		}

		int scalarChannels = referencePose.scalarChannelCount();
		for (Layer layer : layers)
		{
			scalarChannels = Math.max(scalarChannels, layer.pose().scalarChannelCount());
		}
		PPose result = referencePose.copy(scalarChannels);
		List<Layer> ordered = new ArrayList<>(layers);
		ordered.removeIf(layer -> layer.weight() <= 0.0f);
		ordered.sort(Comparator.comparingInt(Layer :: priority));

		for (int offset = 0; offset < ordered.size(); )
		{
			int priority = ordered.get(offset).priority();
			int end = offset + 1;
			while (end < ordered.size() && ordered.get(end).priority() == priority)
			{
				end++;
			}
			mixPriority(result, referencePose, ordered.subList(offset, end));
			offset = end;
		}
		markDirty(referencePose, result);
		return result;
	}

	/**
	 * Creates the two weighted layers forming a crossfade at {@code elapsed} .
	 *
	 * @param outgoing the outgoing
	 * @param incoming the incoming
	 * @param elapsed the elapsed transition time in game ticks
	 * @param duration the duration of the effect or transition in game ticks
	 * @param easing the crossfade progress easing curve
	 * @return the outgoing and incoming layers with complementary eased weights
	 */
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

	/**
	 * Blends resolved poses against their reference without promoting absent channels to identity.
	 * Only the endpoints' channel masks participate; scalar values and visibility are retained.
	 *
	 * @param reference the reference value or channel used by this operation
	 * @param source the pose supplying transform and channel-presence data
	 * @param target the target
	 * @param alpha the normalized interpolation progress
	 * @return the indexed bone pose
	 */
	public static PPose blend(PPose reference, PPose source, PPose target, float alpha)
	{
		if (source.boneCount() != reference.boneCount() || target.boneCount() != reference.boneCount())
		{
			throw new IllegalArgumentException("Blended poses must have equal bone counts");
		}
		alpha = Math.clamp(alpha, 0.0f, 1.0f);
		if (alpha == 0.0f)
		{
			return source.copy();
		}
		if (alpha == 1.0f)
		{
			return target.copy();
		}
		int scalars = Math.max(reference.scalarChannelCount(), Math.max(source.scalarChannelCount(), target.scalarChannelCount()));
		PPose result = new PPose(reference.boneCount(), scalars);
		for (int bone = 0; bone < reference.boneCount(); bone++)
		{
			if (source.hasTranslation(bone) || target.hasTranslation(bone))
			{
				result.translation(bone, new Vector3f(source.hasTranslation(bone) ? source.translation(bone) : reference.translation(bone)).
						lerp(target.hasTranslation(bone) ? target.translation(bone) : reference.translation(bone), alpha));
			}
			if (source.hasRotation(bone) || target.hasRotation(bone))
			{
				Quaternionf rotation = new Quaternionf(source.hasRotation(bone) ? source.rotation(bone) : reference.rotation(bone));
				shortestSlerp(rotation, target.hasRotation(bone) ? target.rotation(bone) : reference.rotation(bone), alpha);
				result.rotation(bone, rotation);
			}
			if (source.hasScale(bone) || target.hasScale(bone))
			{
				result.scale(bone, new Vector3f(source.hasScale(bone) ? source.scale(bone) : reference.scale(bone)).
						lerp(target.hasScale(bone) ? target.scale(bone) : reference.scale(bone), alpha));
			}
			if (source.hasVisibility(bone) || target.hasVisibility(bone))
			{
				result.visibility(bone, source.isVisible(bone) && target.isVisible(bone));
			}
		}
		for (int channel = 0; channel < scalars; channel++)
		{
			boolean from = channel < source.scalarChannelCount() && source.hasScalarChannel(channel);
			boolean to = channel < target.scalarChannelCount() && target.hasScalarChannel(channel);
			if (from || to)
			{
				float base = channel < reference.scalarChannelCount() ? reference.scalarChannel(channel) : 0.0f;
				float start = from ? source.scalarChannel(channel) : base;
				float end = to ? target.scalarChannel(channel) : base;
				result.scalarChannel(channel, start + (end - start) * alpha);
			}
		}
		return result;
	}

	/** Stateful crossfade helper with explicit interruption semantics. */
	public static final class Transition
	{
		private List<Supplier<Layer>> sourceLayers;
		private Layer target;
		private final PPoseMixer mixer;
		private final PPose reference;
		private boolean snapshotBlend;
		private float elapsed;
		private float duration;
		private PPoseEasing easing;

		/**
		 * Uses the initial layer's reference, or identity, and a hierarchy-free mixer.
		 *
		 * @param initial the initial
		 */
		public Transition(Layer initial)
		{
			this(new PPoseMixer(initial.pose().boneCount()),
					initial.referencePose() == null ? new PPose(initial.pose().boneCount(), initial.pose().scalarChannelCount()) : initial.referencePose(), initial);
		}

		/**
		 * Supplies the actual reference/hierarchy for additive and mesh-space snapshot capture.
		 *
		 * @param mixer the mixer
		 * @param reference the reference value or channel used by this operation
		 * @param initial the initial
		 */
		public Transition(PPoseMixer mixer, PPose reference, Layer initial)
		{
			this.mixer = Objects.requireNonNull(mixer);
			this.reference = Objects.requireNonNull(reference).copy();
			this.target = Objects.requireNonNull(initial);
			this.sourceLayers = List.of();
			this.elapsed = 1.0f;
			this.duration = 0.0f;
			this.easing = PPoseEasing.LINEAR;
		}

		/**
		 * Starts a transition to the target layer according to the requested interruption policy.
		 *
		 * @param target the target
		 * @param duration the duration of the effect or transition in game ticks
		 * @param easing the crossfade progress easing curve
		 * @param interruptionPolicy the policy for interrupting an active crossfade
		 * @return whether the new transition was accepted
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
			{
				return false;
			}
			if (duration > 0.0f && interruptionPolicy == PTransitionInterruptionPolicy.RESTART)
			{
				PPoseSnapshot snapshot = new PPoseSnapshot(this.mixer.mix(this.reference, layers()));
				this.sourceLayers = List.of(() -> new Layer(snapshot.sample(), PPoseBlendMode.OVERRIDE, 0, 1.0f));
				this.snapshotBlend = true;
			}
			else
			{
				this.sourceLayers = currentSources();
				this.snapshotBlend = false;
			}
			this.target = target;
			this.elapsed = 0.0f;
			this.duration = Math.max(duration, 0.0f);
			this.easing = easing;
			if (!isTransitioning())
			{
				this.sourceLayers = List.of();
				this.snapshotBlend = false;
			}
			return true;
		}

		/**
		 * Advances the active crossfade by the supplied elapsed tick interval.
		 *
		 * @param delta the elapsed playback interval in game ticks
		 */
		public void advance(float delta)
		{
			this.elapsed = Math.min(this.elapsed + Math.max(delta, 0.0f), this.duration);
			if (!isTransitioning())
			{
				this.sourceLayers = List.of();
				this.snapshotBlend = false;
			}
		}

		/**
		 * Returns whether a crossfade is currently active.
		 *
		 * @return whether a crossfade is currently active
		 */
		public boolean isTransitioning()
		{
			return this.elapsed < this.duration;
		}

		private List<Supplier<Layer>> currentSources()
		{
			Layer currentTarget = this.target;
			if (!isTransitioning())
			{
				return List.of(() -> currentTarget);
			}
			float alpha = this.easing.transform(this.elapsed / this.duration);
			if (this.snapshotBlend)
			{
				Supplier<Layer> source = this.sourceLayers.getFirst();
				return List.of(() -> resolvedBlend(source.get().pose(), currentTarget, alpha));
			}
			List<Supplier<Layer>> sources = new ArrayList<>(this.sourceLayers.size() + 1);
			for (Supplier<Layer> source : this.sourceLayers)
			{
				sources.add(() ->
				{
					Layer layer = source.get();
					return layer.withWeight(layer.weight() * (1.0f - alpha));
				});
			}
			sources.add(() -> currentTarget.withWeight(currentTarget.weight() * alpha));
			return List.copyOf(sources);
		}

		private Layer resolvedBlend(PPose snapshot, Layer target, float alpha)
		{
			PPose targetPose = this.mixer.mix(this.reference, List.of(target));
			PPose pose = blend(this.reference, snapshot, targetPose, alpha);
			return new Layer(pose, PPoseBlendMode.OVERRIDE, target.priority(), 1.0f);
		}

		/**
		 * Returns the layers and weights representing the current transition state.
		 *
		 * @return the layers and weights representing the current transition state
		 */
		public List<Layer> layers()
		{
			if (!isTransitioning())
			{
				return List.of(this.target);
			}
			float alpha = this.easing.transform(this.elapsed / this.duration);
			if (this.snapshotBlend)
			{
				return List.of(resolvedBlend(this.sourceLayers.getFirst().get().pose(), this.target, alpha));
			}
			List<Layer> layers = new ArrayList<>(this.sourceLayers.size() + 1);
			for (Supplier<Layer> source : this.sourceLayers)
			{
				Layer layer = source.get();
				layers.add(layer.withWeight(layer.weight() * (1.0f - alpha)));
			}
			layers.add(this.target.withWeight(this.target.weight() * alpha));
			return List.copyOf(layers);
		}
	}

	private void mixPriority(PPose result, PPose reference, List<Layer> layers)
	{
		for (int channel = 0; channel < result.scalarChannelCount(); channel++)
		{
			float total = 0.0f;
			float value = 0.0f;
			for (Layer layer : layers)
			{
				if (layer.mode() == PPoseBlendMode.OVERRIDE && channel < layer.pose().scalarChannelCount() && layer.pose().hasScalarChannel(channel))
				{
					value += layer.pose().scalarChannel(channel) * layer.weight();
					total += layer.weight();
				}
			}
			if (total > 0.0f)
			{
				float initial = result.scalarChannel(channel);
				result.scalarChannel(channel, initial + (value / total - initial) * Math.min(total, 1.0f));
			}
		}
		for (int bone = 0; bone < this.boneCount; bone++)
		{
			mixOverrides(result, layers, bone);
			for (Layer layer : layers)
			{
				if (layer.boneWeight(bone) > 0.0f)
				{
					result.includeChannels(layer.pose(), bone);
					if (layer.pose().hasVisibility(bone))
					{
						result.visibility(bone, result.isVisible(bone) && layer.pose().isVisible(bone));
					}
				}
			}
			for (Layer layer : layers)
			{
				if (layer.mode() == PPoseBlendMode.OVERRIDE)
				{
					continue;
				}
				float weight = layer.boneWeight(bone);
				if (weight <= 0.0f)
				{
					continue;
				}
				apply(result, reference, layer, bone, weight);
			}
		}
	}

	private void mixOverrides(PPose result, List<Layer> layers, int bone)
	{
		float translationWeight = 0.0f;
		float scaleWeight = 0.0f;
		float rotationWeight = 0.0f;
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
			{
				continue;
			}
			float weight = layer.boneWeight(bone);
			if (weight <= 0.0f)
			{
				continue;
			}
			PPose pose = layer.pose();
			if (pose.hasTranslation(bone))
			{
				translation.fma(weight, pose.translation(bone));
				translationWeight += weight;
			}
			if (pose.hasScale(bone))
			{
				scale.fma(weight, pose.scale(bone));
				scaleWeight += weight;
			}
			if (pose.hasRotation(bone))
			{
				Quaternionf rotation = pose.rotation(bone);
				if (anchor == null)
				{
					anchor = rotation;
				}
				float sign = anchor.dot(rotation) < 0.0f ? -1.0f : 1.0f;
				qx += rotation.x * weight * sign;
				qy += rotation.y * weight * sign;
				qz += rotation.z * weight * sign;
				qw += rotation.w * weight * sign;
				rotationWeight += weight;
			}
		}
		if (translationWeight > 0.0f)
		{
			result.translation(bone).lerp(translation.div(translationWeight), Math.min(translationWeight, 1.0f));
		}
		if (scaleWeight > 0.0f)
		{
			result.scale(bone).lerp(scale.div(scaleWeight), Math.min(scaleWeight, 1.0f));
		}
		if (rotationWeight > 0.0f)
		{
			Quaternionf rotation = new Quaternionf(qx, qy, qz, qw).normalize();
			shortestSlerp(result.rotation(bone), rotation, Math.min(rotationWeight, 1.0f));
		}
	}

	private void apply(PPose result, PPose reference, Layer layer, int bone, float weight)
	{
		PPose pose = layer.pose();
		PPose layerReference = layer.referencePose() == null ? reference : layer.referencePose();
		Vector3f translationDelta = pose.hasTranslation(bone) ? new Vector3f(pose.translation(bone)).sub(layerReference.translation(bone)) : new Vector3f();
		Quaternionf rotationDelta = pose.hasRotation(bone) ? new Quaternionf(layerReference.rotation(bone)).invert().premul(pose.rotation(bone)).normalize() : new Quaternionf();
		Vector3f scaleFactor = pose.hasScale(bone) ? divide(pose.scale(bone), layerReference.scale(bone)) : new Vector3f(1.0f);
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
		{
			delta.invert();
		}
		result.rotation(bone).premul(delta).normalize();
		Vector3f factor = weightedScale(scale, weight);
		if (inverse)
		{
			factor.set(safeInverse(factor.x), safeInverse(factor.y), safeInverse(factor.z));
		}
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
		{
			return result;
		}
		for (int parent = this.parents[bone]; parent >= 0; parent = this.parents[parent])
		{
			result.premul(pose.rotation(parent));
		}
		return result.normalize();
	}


	private static void markDirty(PPose reference, PPose result)
	{
		for (int bone = 0; bone < result.boneCount(); bone++)
			if (result.translation(bone).distanceSquared(reference.translation(bone)) > EPSILON * EPSILON ||
					Math.abs(Math.abs(result.rotation(bone).dot(reference.rotation(bone))) - 1.0f) > EPSILON ||
					result.scale(bone).distanceSquared(reference.scale(bone)) > EPSILON * EPSILON)
				result.markDirty(bone);
	}

	private static void shortestSlerp(Quaternionf destination, Quaternionf target, float alpha)
	{
		Quaternionf shortestTarget = new Quaternionf(target);
		if (destination.dot(shortestTarget) < 0.0f)
		{
			shortestTarget.set(-shortestTarget.x, -shortestTarget.y, -shortestTarget.z, -shortestTarget.w);
		}
		destination.slerp(shortestTarget, alpha).normalize();
	}

	private static Quaternionf weightedRotation(Quaternionf rotation, float weight)
	{
		Quaternionf shortestRotation = new Quaternionf(rotation);
		if (shortestRotation.w < 0.0f)
		{
			shortestRotation.set(-shortestRotation.x, -shortestRotation.y, -shortestRotation.z, -shortestRotation.w);
		}
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

	/**
	 * Supplies the contribution weight of an individual bone in a pose layer.
	 */
	@FunctionalInterface
	public interface BoneWeight
	{
		/**
		 * Assigns full contribution to every bone.
		 */
		BoneWeight FULL = boneIndex -> 1.0f;

		/**
		 * Returns the layer weight for the supplied bone index.
		 *
		 * @param boneIndex the zero-based baked-model bone index
		 * @return the layer weight for the supplied bone index
		 */
		float weight(int boneIndex);
	}

	/**
	 * Pose contribution with a reference pose, bone mask, blend mode, priority and overall weight.
	 *
	 * @param pose the indexed bone pose
	 * @param referencePose the reference pose used to calculate layer deltas; may be {@code null}
	 * @param mask the mask selecting participating bones or player parts
	 * @param boneWeight the per-bone contribution weight provider
	 * @param mode the rule used to combine the layer with the accumulated value
	 * @param priority the ordering priority of the contribution
	 * @param weight the contribution weight
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
		 * Creates a weighted pose layer with the supplied blending and bone-selection policy.
		 *
		 * @param pose the indexed bone pose
		 * @param referencePose the reference pose used to calculate layer deltas; may be {@code null}
		 * @param mask the mask selecting participating bones or player parts
		 * @param boneWeight the per-bone contribution weight provider
		 * @param mode the rule used to combine the layer with the accumulated value
		 * @param priority the ordering priority of the contribution
		 * @param weight the contribution weight
		 */
		public Layer
		{
			pose = Objects.requireNonNull(pose);
			mask = mask == null ? allBones(pose.boneCount()) : (BitSet) mask.clone();
			boneWeight = boneWeight == null ? BoneWeight.FULL : boneWeight;
			mode = Objects.requireNonNull(mode);
			weight = Math.clamp(weight, 0.0f, 1.0f);
			if (referencePose != null && referencePose.boneCount() != pose.boneCount())
			{
				throw new IllegalArgumentException("Layer and reference poses must have equal bone counts");
			}
		}

		/**
		 * Creates a weighted pose layer with the supplied blending and bone-selection policy.
		 *
		 * @param pose the indexed bone pose
		 * @param mode the rule used to combine the layer with the accumulated value
		 * @param priority the ordering priority of the contribution
		 * @param weight the contribution weight
		 */
		public Layer(PPose pose, PPoseBlendMode mode, int priority, float weight)
		{
			this(pose, null, null, null, mode, priority, weight);
		}

		/**
		 * Returns the effective contribution weight for the supplied bone index.
		 *
		 * @param boneIndex the zero-based baked-model bone index
		 * @return the effective contribution weight for the supplied bone index
		 */
		public float boneWeight(int boneIndex)
		{
			return this.mask.get(boneIndex) ? Math.clamp(this.weight * this.boneWeight.weight(boneIndex), 0.0f, 1.0f) : 0.0f;
		}

		/**
		 * Returns a layer copy with a different overall contribution weight.
		 *
		 * @param weight the contribution weight
		 * @return a layer copy with a different overall contribution weight
		 */
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
