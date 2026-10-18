/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.data.gecko.MolangParser;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves accumulated controller animation into local, model-space and relative bone transforms.
 *
 * @param <T> the animatable owner type
 */
public final class PAnimationPoseResolver<T extends PAnimatable<T>>
{
	private final PBakedModel model;
	private final Collection<PAnimationController<T>> controllers;
	private final MolangContextProvider<T> molangContexts;
	private final float partialTick;
	private final PPose localPose;
	private final Set<String> activeBones = new HashSet<>();
	private final PModelPose modelPose;
	private final PModelPose bindModelPose;

	/**
	 * Creates pose resolution for the supplied model, ordered controllers and render sample.
	 *
	 * @param model the baked model supplying indexed bones and animations
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	public PAnimationPoseResolver(PBakedModel model,
	                              Collection<PAnimationController<T>> controllers,
	                              MolangContextProvider<T> molangContexts,
	                              float partialTick)
	{
		this.model = Objects.requireNonNull(model);
		this.controllers = Objects.requireNonNull(controllers);
		this.molangContexts = Objects.requireNonNull(molangContexts);
		this.partialTick = partialTick;
		this.localPose = PAnimationRuntime.evaluate(this.model, this.controllers, this.molangContexts, this.partialTick, this.activeBones);
		this.modelPose = new PModelPose(this.model.boneCount());
		this.modelPose.update(this.model, this.localPose);
		this.bindModelPose = new PModelPose(this.model.boneCount());
		this.bindModelPose.update(this.model, this.model.bindPose());
	}

	/**
	 * Resolves the selected bone into local and accumulated animation transform data.
	 *
	 * @param boneName the name of a bone in the baked model hierarchy
	 * @return the selected bone's resolved pose data, or {@code null} when the requested bone or reference bone does not exist
	 */
	public @Nullable BonePose resolve(String boneName)
	{
		int index = this.model.boneIndex(boneName);
		return index < 0 ? null : resolve(this.model.bone(index));
	}

	/**
	 * Resolves the selected bone into local and accumulated animation transform data.
	 *
	 * @param bone the baked bone being sampled or submitted
	 * @return the selected bone's resolved pose data
	 */
	public BonePose resolve(PBakedBone bone)
	{
		int index = this.model.boneIndex(bone);
		Vector3f baseTranslation = bone.basePosition();
		Quaternionf baseRotation = bone.baseRotation();
		Vector3f localTranslation = this.localPose.translation(index);
		Quaternionf localRotation = this.localPose.rotation(index);
		Vector3f localScale = this.localPose.scale(index);
		boolean animated = this.localPose.isDirty(index);
		return new BonePose(
				new BoneFrame(new Vector3f(localTranslation).sub(baseTranslation), new Quaternionf(baseRotation).invert().premul(localRotation), new Vector3f(localScale)),
				new BoneFrame(new Vector3f(localTranslation), new Quaternionf(localRotation), new Vector3f(localScale)),
				this.bindModelPose.transform(index),
				this.modelPose.transform(index),
				animated,
				animated,
				animated);
	}

	/**
	 * Returns whether the selected bone is visible in the sampled pose.
	 *
	 * @param bone the baked bone being sampled or submitted
	 * @return whether the selected bone is visible in the sampled pose
	 */
	public boolean isVisible(PBakedBone bone)
	{
		return this.localPose.isVisible(this.model.boneIndex(bone));
	}

	/**
	 * Returns the bone names to which active controller animation can contribute.
	 *
	 * @return the bone names to which active controller animation can contribute
	 */
	public Set<String> activeAnimationBones()
	{
		return Set.copyOf(this.activeBones);
	}

	/**
	 * Returns the named bone's sampled animation delta relative to the selected root bone.
	 *
	 * @param boneName the name of a bone in the baked model hierarchy
	 * @param rootBoneName the bone used as the root-motion or relative-transform reference; may be {@code null}
	 * @return the named bone's sampled animation delta relative to the selected root bone, or {@code null} when the requested bone or reference bone does not exist
	 */
	public @Nullable AnimationDelta animationDelta(String boneName, @Nullable String rootBoneName)
	{
		int boneIndex = this.model.boneIndex(boneName);

		if (boneIndex < 0)
			return null;

		PTransform current;
		PTransform bind;

		if (rootBoneName == null || boneName.equals(rootBoneName))
		{
			current = modelTransform(boneIndex);
			bind = bindModelTransform(boneIndex);
		}
		else
		{
			int rootIndex = this.model.boneIndex(rootBoneName);

			if (rootIndex < 0)
				return null;

			current = relativeTransform(
					boneIndex,
					rootIndex);

			bind = relativeBindTransform(
					boneIndex,
					rootIndex);
		}

		Vector3f currentTranslation = current.translation();
		Vector3f bindTranslation = bind.translation();

		Vector3f translation =
				currentTranslation.sub(bindTranslation);

		Quaternionf currentRotation = current.rotation();
		Quaternionf bindRotation = bind.rotation();

		Quaternionf rotation =
				new Quaternionf(bindRotation).
						invert().
						premul(currentRotation);

		Vector3f currentScale = current.scale();
		Vector3f bindScale = bind.scale();

		Vector3f scale = new Vector3f(
				ratio(currentScale.x, bindScale.x),
				ratio(currentScale.y, bindScale.y),
				ratio(currentScale.z, bindScale.z));

		return new AnimationDelta(
				translation,
				rotation,
				scale,
				translation.lengthSquared() > 1.0e-10f,
				Math.abs(currentRotation.dot(bindRotation)) < 0.999999f,
				Math.abs(scale.x - 1.0f) > 1.0e-5f ||
						Math.abs(scale.y - 1.0f) > 1.0e-5f ||
						Math.abs(scale.z - 1.0f) > 1.0e-5f);
	}

	/**
	 * Returns the sampled model-space transform of the selected bone.
	 *
	 * @param boneName the name of a bone in the baked model hierarchy
	 * @return the sampled model-space transform of the selected bone, or {@code null} when the requested bone or reference bone does not exist
	 */
	public @Nullable PTransform modelTransform(String boneName)
	{
		int index = this.model.boneIndex(boneName);
		return index < 0 ? null : modelTransform(index);
	}

	/**
	 * Returns the sampled model-space transform of the selected bone.
	 *
	 * @param bone the baked bone being sampled or submitted
	 * @return the sampled model-space transform of the selected bone
	 */
	public PTransform modelTransform(PBakedBone bone)
	{
		return modelTransform(this.model.boneIndex(bone));
	}

	/**
	 * Returns the canonical resolved MODEL-space transform for a bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @return the canonical resolved MODEL-space transform for a bone
	 */
	public PTransform modelTransform(int boneIndex)
	{
		return this.modelPose.transform(boneIndex);
	}

	/**
	 * Returns the selected bone's unanimated model-space transform.
	 *
	 * @param boneName the name of a bone in the baked model hierarchy
	 * @return the selected bone's unanimated model-space transform, or {@code null} when the requested bone or reference bone does not exist
	 */
	public @Nullable PTransform bindModelTransform(String boneName)
	{
		int index = this.model.boneIndex(boneName);
		return index < 0 ? null : bindModelTransform(index);
	}

	/**
	 * Returns the selected bone's unanimated model-space transform.
	 *
	 * @param bone the baked bone being sampled or submitted
	 * @return the selected bone's unanimated model-space transform
	 */
	public PTransform bindModelTransform(PBakedBone bone)
	{
		return bindModelTransform(this.model.boneIndex(bone));
	}

	/**
	 * Returns the selected bone's unanimated model-space transform.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @return the selected bone's unanimated model-space transform
	 */
	public PTransform bindModelTransform(int boneIndex)
	{
		return this.bindModelPose.transform(boneIndex);
	}

	/**
	 * Returns the sampled bone transform relative to the selected reference bone.
	 *
	 * @param boneName the name of a bone in the baked model hierarchy
	 * @param referenceBoneName the reference bone used to calculate a relative transform
	 * @return the sampled bone transform relative to the selected reference bone, or {@code null} when the requested bone or reference bone does not exist
	 */
	public @Nullable PTransform relativeTransform(String boneName, String referenceBoneName)
	{
		int boneIndex = this.model.boneIndex(boneName);
		int referenceIndex = this.model.boneIndex(referenceBoneName);

		if (boneIndex < 0 || referenceIndex < 0)
			return null;

		return relativeTransform(boneIndex, referenceIndex);
	}

	/**
	 * Returns the sampled bone transform relative to the selected reference bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param referenceIndex the zero-based reference bone index
	 * @return the sampled bone transform relative to the selected reference bone
	 */
	public PTransform relativeTransform(int boneIndex, int referenceIndex)
	{
		return this.modelPose.transform(referenceIndex).inverse().compose(this.modelPose.transform(boneIndex));
	}

	/**
	 * Returns the unanimated bone transform relative to the selected reference bone.
	 *
	 * @param boneName the name of a bone in the baked model hierarchy
	 * @param referenceBoneName the reference bone used to calculate a relative transform
	 * @return the unanimated bone transform relative to the selected reference bone, or {@code null} when the requested bone or reference bone does not exist
	 */
	public @Nullable PTransform relativeBindTransform(String boneName, String referenceBoneName)
	{
		int boneIndex = this.model.boneIndex(boneName);
		int referenceIndex = this.model.boneIndex(referenceBoneName);

		if (boneIndex < 0 || referenceIndex < 0)
			return null;

		return relativeBindTransform(boneIndex, referenceIndex);
	}

	/**
	 * Returns the unanimated bone transform relative to the selected reference bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param referenceIndex the zero-based reference bone index
	 * @return the unanimated bone transform relative to the selected reference bone
	 */
	public PTransform relativeBindTransform(int boneIndex, int referenceIndex)
	{
		return this.bindModelPose.transform(referenceIndex).inverse().compose(this.bindModelPose.transform(boneIndex));
	}

	/**
	 * Returns the unanimated model-space transform of the named bone's parent.
	 *
	 * @param boneName the name of a bone in the baked model hierarchy
	 * @return the unanimated model-space transform of the named bone's parent, or {@code null} when the requested bone or reference bone does not exist
	 */
	@Nullable
	public PTransform bindParentTransform(String boneName)
	{
		int boneIndex =
				this.model.boneIndex(boneName);

		if (boneIndex < 0)
			return null;

		int parentIndex =
				this.model.parentIndex(boneIndex);

		return parentIndex < 0 ?
				PTransform.IDENTITY :
				this.bindModelPose.transform(parentIndex);
	}

	/**
	 * Samples and combines the supplied controllers into one bone-local pose.
	 *
	 * @param <T> the animatable owner type
	 * @param bone the baked bone being sampled or submitted
	 * @param model the baked model supplying indexed bones and animations
	 * @param controllers the animation controllers in pose composition order
	 * @param molangContexts the frame-specific Molang context of each controller
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @return the combined local transform and channel presence of the selected bone
	 */
	public static <T extends PAnimatable<T>> LocalPose resolveLocal(PBakedBone bone,
	                                                                PBakedModel model,
	                                                                Collection<PAnimationController<T>> controllers,
	                                                                MolangContextProvider<T> molangContexts,
	                                                                float partialTick)
	{
		Vector3f translation = new Vector3f(bone.basePosition());
		Quaternionf rotation = new Quaternionf(bone.baseRotation());
		Vector3f scale = new Vector3f(1.0f);
		Vector3f animationTranslation = new Vector3f();
		Quaternionf animationRotation = new Quaternionf();
		Vector3f animationScale = new Vector3f(1.0f);
		boolean hasTranslation = false;
		boolean hasRotation = false;
		boolean hasScale = false;

		for (PAnimationController<T> controller : controllers)
		{
			PAnimationStagePlayback stage = controller.getCurrentStage();
			if (stage == null || stage.isWaiting())
				continue;

			PAnimation animation = model.animations().get(stage.animationName());
			if (animation == null)
				continue;

			PBoneAnimation boneAnimation = animation.boneAnimations().get(bone.name());
			if (boneAnimation == null)
				continue;

			BoneFrame frame = controller.calculateBoneTransformations(
					bone.name(),
					model,
					partialTick,
					molangContexts.context(controller, partialTick),
					new BoneFrame(new Vector3f(translation), new Quaternionf(rotation), new Vector3f(scale)));
			if (frame == null)
				continue;

			if (boneAnimation.hasChannel(PLibRegistration.AnimationChannelReg.POSITION))
			{
				translation.add(frame.translation());
				animationTranslation.add(frame.translation());
				hasTranslation = true;
			}
			if (boneAnimation.hasChannel(PLibRegistration.AnimationChannelReg.ROTATION))
			{
				rotation.premul(frame.rotation());
				animationRotation.premul(frame.rotation());
				hasRotation = true;
			}
			if (boneAnimation.hasChannel(PLibRegistration.AnimationChannelReg.SCALE))
			{
				scale.mul(frame.scale());
				animationScale.mul(frame.scale());
				hasScale = true;
			}
		}

		return new LocalPose(
				new BoneFrame(animationTranslation, animationRotation, animationScale),
				new BoneFrame(translation, rotation, scale),
			hasTranslation,
			hasRotation,
			hasScale);
	}

	/**
	 * Creates a context provider using controller-owned Molang state and interpolated animation time.
	 *
	 * @param <T> the animatable owner type
	 * @return the provider using controller-owned Molang contexts and interpolated animation time
	 */
	public static <T extends PAnimatable<T>> MolangContextProvider<T> defaultContexts()
	{
		return (controller, partialTick) -> new MolangParser.Context().
				query("anim_time", PAnimationEvaluationContext.ticksToSeconds(controller.getInterpolatedTime(partialTick))).
				randomSeed(0L);
	}

	/**
	 * Calculates a scale ratio while avoiding division by a zero bind value.
	 *
	 * @param value the value written by this operation
	 * @param base the accumulated channel value before the current layer
	 * @return the scale ratio, using the implementation's zero-bind fallback
	 */
	private static float ratio(float value, float base)
	{
		return Math.abs(base) < 1.0e-6f ? value : value / base;
	}

	/**
	 * Supplies per-controller Molang values for one interpolated animation sample.
	 *
	 * @param <T> the animatable owner type
	 */
	@FunctionalInterface
/**
 * Defines the contract for molang context provider.
 */
	public interface MolangContextProvider<T extends PAnimatable<T>>
	{
		/**
		 * Returns the Molang values used to sample the supplied controller at this render fraction.
		 *
		 * @param controller the controller supplying playback and persistent expression state
		 * @param partialTick the interpolation fraction between the previous and current game ticks
		 * @return the Molang values used to sample the supplied controller at this render fraction
		 */
		MolangParser.Context context(PAnimationController<T> controller, float partialTick);
	}

/**
	 * Accumulated local bone transform and its contributing animation channels.
	 *
	 * @param animationTransform the animation transform
	 * @param localTransform the local transform
	 * @param hasTranslation whether the translation channel contributes an animated value
	 * @param hasRotation whether the rotation channel contributes an animated value
	 * @param hasScale whether the scale channel contributes an animated value
	 */
	public record LocalPose(BoneFrame animationTransform,
	                        BoneFrame localTransform,
	                        boolean hasTranslation,
	                        boolean hasRotation,
	                        boolean hasScale)
	{
	}

/**
	 * Resolved animated bone transform with channel-presence information.
	 *
	 * @param animationTransform the animation transform
	 * @param localTransform the local transform
	 * @param bindTransform the bind transform
	 * @param modelTransform the model transform
	 * @param hasTranslation whether the translation channel contributes an animated value
	 * @param hasRotation whether the rotation channel contributes an animated value
	 * @param hasScale whether the scale channel contributes an animated value
	 */
	public record BonePose(BoneFrame animationTransform,
	                       BoneFrame localTransform,
	                       PTransform bindTransform,
	                       PTransform modelTransform,
	                       boolean hasTranslation,
	                       boolean hasRotation,
	                       boolean hasScale)
	{
		/**
		 * Returns whether any local transform channel contributes animation.
		 *
		 * @return whether any local transform channel contributes animation
		 */
		public boolean isAnimated()
		{
			return this.hasTranslation || this.hasRotation || this.hasScale;
		}
	}

/**
	 * Animation-only bone transform relative to a selected root reference.
	 *
	 * @param translation the local translation vector
	 * @param rotation the local quaternion rotation
	 * @param scale the local scale vector
	 * @param hasTranslation whether the translation channel contributes an animated value
	 * @param hasRotation whether the rotation channel contributes an animated value
	 * @param hasScale whether the scale channel contributes an animated value
	 */
	public record AnimationDelta(Vector3f translation,
	                             Quaternionf rotation,
	                             Vector3f scale,
	                             boolean hasTranslation,
	                             boolean hasRotation,
	                             boolean hasScale)
	{
		/**
		 * Returns whether this delta contains an animated transform contribution.
		 *
		 * @return whether this delta contains an animated transform contribution
		 */
		public boolean isAnimated()
		{
			return this.hasTranslation || this.hasRotation || this.hasScale;
		}
	}
}
