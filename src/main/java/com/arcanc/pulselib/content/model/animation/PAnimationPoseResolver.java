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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Objects;

public final class PAnimationPoseResolver<T extends PAnimatable<T>>
{
	private final PBakedModel model;
	private final Collection<PAnimationController<T>> controllers;
	private final MolangContextProvider<T> molangContexts;
	private final float partialTick;
	private final PPose localPose;
	private final PModelPose modelPose;
	private final PModelPose bindModelPose;

	public PAnimationPoseResolver(PBakedModel model,
	                              Collection<PAnimationController<T>> controllers,
	                              MolangContextProvider<T> molangContexts,
	                              float partialTick)
	{
		this.model = Objects.requireNonNull(model);
		this.controllers = Objects.requireNonNull(controllers);
		this.molangContexts = Objects.requireNonNull(molangContexts);
		this.partialTick = partialTick;
		this.localPose = PAnimationRuntime.evaluate(this.model, this.controllers, this.molangContexts, this.partialTick);
		this.modelPose = new PModelPose(this.model.boneCount());
		this.modelPose.update(this.model, this.localPose);
		this.bindModelPose = new PModelPose(this.model.boneCount());
		this.bindModelPose.update(this.model, this.model.bindPose());
	}

	public @Nullable BonePose resolve(String boneName)
	{
		int index = this.model.boneIndex(boneName);
		return index < 0 ? null : resolve(this.model.bone(index));
	}

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
				new Matrix4f(this.bindModelPose.transform(index)),
				new Matrix4f(this.modelPose.transform(index)),
				animated,
				animated,
				animated);
	}
	
	public @Nullable AnimationDelta animationDelta(String boneName, @Nullable String rootBoneName)
	{
		int boneIndex = this.model.boneIndex(boneName);
		
		if (boneIndex < 0)
			return null;
		
		Matrix4f current;
		Matrix4f bind;
		
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
		
		Vector3f currentTranslation =
				current.getTranslation(new Vector3f());
		
		Vector3f bindTranslation =
				bind.getTranslation(new Vector3f());
		
		Vector3f translation =
				currentTranslation.sub(bindTranslation);
		
		Quaternionf currentRotation =
				current.getUnnormalizedRotation(new Quaternionf());
		
		Quaternionf bindRotation =
				bind.getUnnormalizedRotation(new Quaternionf());
		
		Quaternionf rotation =
				new Quaternionf(bindRotation).
						invert().
						premul(currentRotation);
		
		Vector3f currentScale =
				current.getScale(new Vector3f());
		
		Vector3f bindScale =
				bind.getScale(new Vector3f());
		
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
	
	public @Nullable Matrix4f modelTransform(String boneName)
	{
		int index = this.model.boneIndex(boneName);
		return index < 0 ? null : modelTransform(index);
	}
	
	public Matrix4f modelTransform(PBakedBone bone)
	{
		return modelTransform(this.model.boneIndex(bone));
	}
	
	public Matrix4f modelTransform(int boneIndex)
	{
		return new Matrix4f(this.modelPose.transform(boneIndex));
	}
	
	public @Nullable Matrix4f bindModelTransform(String boneName)
	{
		int index = this.model.boneIndex(boneName);
		return index < 0 ? null : bindModelTransform(index);
	}
	
	public Matrix4f bindModelTransform(PBakedBone bone)
	{
		return bindModelTransform(this.model.boneIndex(bone));
	}
	
	public Matrix4f bindModelTransform(int boneIndex)
	{
		return new Matrix4f(this.bindModelPose.transform(boneIndex));
	}
	
	public @Nullable Matrix4f relativeTransform(String boneName, String referenceBoneName)
	{
		int boneIndex = this.model.boneIndex(boneName);
		int referenceIndex = this.model.boneIndex(referenceBoneName);
		
		if (boneIndex < 0 || referenceIndex < 0)
			return null;
		
		return relativeTransform(boneIndex, referenceIndex);
	}
	
	public Matrix4f relativeTransform(int boneIndex, int referenceIndex)
	{
		return new Matrix4f(this.modelPose.transform(referenceIndex)).
				invert().
				mul(this.modelPose.transform(boneIndex));
	}
	
	public @Nullable Matrix4f relativeBindTransform(String boneName, String referenceBoneName)
	{
		int boneIndex = this.model.boneIndex(boneName);
		int referenceIndex = this.model.boneIndex(referenceBoneName);
		
		if (boneIndex < 0 || referenceIndex < 0)
			return null;
		
		return relativeBindTransform(boneIndex, referenceIndex);
	}
	
	public Matrix4f relativeBindTransform(int boneIndex, int referenceIndex)
	{
		return new Matrix4f(this.bindModelPose.transform(referenceIndex)).
				invert().
				mul(this.bindModelPose.transform(boneIndex));
	}
	
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
			PRawAnimation.AnimationStage stage = controller.getCurrentStage();
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

	public static <T extends PAnimatable<T>> MolangContextProvider<T> defaultContexts()
	{
		return (controller, partialTick) -> new MolangParser.Context().
				query("anim_time", controller.getInterpolatedTime(partialTick) / 20.0f).
				randomSeed(0L);
	}

	private static void apply(Matrix4f matrix, BoneFrame frame)
	{
		matrix.translate(frame.translation()).rotate(frame.rotation()).scale(frame.scale());
	}

	private static float ratio(float value, float base)
	{
		return Math.abs(base) < 1.0e-6f ? value : value / base;
	}

	@FunctionalInterface
	public interface MolangContextProvider<T extends PAnimatable<T>>
	{
		MolangParser.Context context(PAnimationController<T> controller, float partialTick);
	}

	public record LocalPose(BoneFrame animationTransform,
	                        BoneFrame localTransform,
	                        boolean hasTranslation,
	                        boolean hasRotation,
	                        boolean hasScale)
	{
	}

	public record BonePose(BoneFrame animationTransform,
	                       BoneFrame localTransform,
	                       Matrix4f bindTransform,
	                       Matrix4f modelTransform,
	                       boolean hasTranslation,
	                       boolean hasRotation,
	                       boolean hasScale)
	{
		public boolean isAnimated()
		{
			return this.hasTranslation || this.hasRotation || this.hasScale;
		}
	}

	public record AnimationDelta(Vector3f translation,
	                             Quaternionf rotation,
	                             Vector3f scale,
	                             boolean hasTranslation,
	                             boolean hasRotation,
	                             boolean hasScale)
	{
		public boolean isAnimated()
		{
			return this.hasTranslation || this.hasRotation || this.hasScale;
		}
	}
}
