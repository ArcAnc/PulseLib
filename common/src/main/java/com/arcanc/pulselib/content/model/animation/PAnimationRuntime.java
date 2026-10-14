/**
 * @author ArcAnc
 * Created at: 05.08.2026
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class PAnimationRuntime
{
	private PAnimationRuntime()
	{
	}

	public static <T extends PAnimatable<T>> PPose evaluate(PBakedModel model,
	                                                        Collection<PAnimationController<T>> controllers,
	                                                        PAnimationPoseResolver.MolangContextProvider<T> contexts,
	                                                        float partialTick)
	{
		return evaluate(model, controllers, contexts, partialTick, null);
	}

	static <T extends PAnimatable<T>> PPose evaluate(PBakedModel model,
	                                               Collection<PAnimationController<T>> controllers,
	                                               PAnimationPoseResolver.MolangContextProvider<T> contexts,
	                                               float partialTick, @Nullable Set<String> activeBones)
	{
		PPose pose = model.bindPose();
		BitSet affectedBones = new BitSet(model.boneCount());
		for (PAnimationController<T> controller : controllers)
		{
			if (controller.isStopped())
			{
				continue;
			}
			PAnimationGraphRuntime graph = controller.graphRuntime();
			if (graph != null)
			{
				MolangParser.Context supplied = contexts.context(controller, partialTick);
				MolangParser.Context context = controller.persistentMolangContext();
				if (supplied != context)
				{
					context.copyFrameValuesFrom(supplied);
				}
				PPose delta = graph.samplePose(model, partialTick, context, pose);
				for (int bone = 0; bone < model.boneCount(); bone++)
				{
					if (activeBones != null && (delta.isValid(bone) || delta.hasVisibility(bone)))
					{
						activeBones.add(model.bone(bone).name());
					}
					if (delta.isValid(bone))
					{
						pose.translation(bone).add(delta.translation(bone));
						pose.rotation(bone).premul(delta.rotation(bone));
						pose.scale(bone).mul(delta.scale(bone));
						affectedBones.set(bone);
					}
					if (delta.hasVisibility(bone))
					{
						pose.visibility(bone, pose.isVisible(bone) && delta.isVisible(bone));
					}
				}
				continue;
			}
			PCompiledAnimation animation = animation(model, controller);
			if (animation == null)
			{
				continue;
			}
			if (activeBones != null)
			{
				activeBones.addAll(animation.animation().boneAnimations().keySet());
				activeBones.addAll(animation.animation().visibilityTracks().keySet());
			}
			for (int bone = 0; bone < model.boneCount(); bone++)
			{
				if (animation.animation().visibilityTracks().containsKey(model.bone(bone).name()))
				{
					pose.visibility(bone, pose.isVisible(bone) && animation.animation().isBoneVisible(model.bone(bone).name(), controller.getInterpolatedTime(partialTick)));
				}
				PBoneAnimation boneAnimation = animation.boneAnimation(bone);
				if (boneAnimation == null)
				{
					continue;
				}
				BoneFrame frame = controller.calculateBoneTransformations(animation, bone, partialTick, contexts.context(controller, partialTick),
						new BoneFrame(new Vector3f(pose.translation(bone)), new Quaternionf(pose.rotation(bone)), new Vector3f(pose.scale(bone))));
				if (frame != null)
				{
					pose.translation(bone).add(frame.translation());
					pose.rotation(bone).premul(frame.rotation());
					pose.scale(bone).mul(frame.scale());
					affectedBones.set(bone);
				}
			}
		}
		for (int bone = affectedBones.nextSetBit(0); bone >= 0; bone = affectedBones.nextSetBit(bone + 1))
		{
			if (differsFromBind(model, bone, pose.translation(bone), pose.rotation(bone), pose.scale(bone)))
			{
				pose.markDirty(bone);
			}
		}
		return pose;
	}

	/** Captures the requesting graph's base using current tick time and preceding controllers. */
	public static <T extends PAnimatable<T>> PPose captureGraphBase(PBakedModel model, PAnimationController<T> controller,
	                                                               Collection<PAnimationController<T>> controllers)
	{
		return captureGraphBase(model, controller, controllers, (candidate, partialTick) -> new MolangParser.Context().
				copyFrameValuesFrom(candidate.persistentMolangContext()).
				query("anim_time", candidate.getInterpolatedTime(partialTick) / 20.0f));
	}

	/** Resolves current procedural queries for the requesting graph and preceding controllers. */
	public static <T extends PAnimatable<T>> PPose captureGraphBase(PBakedModel model, PAnimationController<T> controller,
	                                                               Collection<PAnimationController<T>> controllers,
	                                                               PAnimationPoseResolver.MolangContextProvider<T> contexts)
	{
		List<PAnimationController<T>> preceding = new ArrayList<>();
		for (PAnimationController<T> candidate : controllers)
		{
			if (candidate == controller)
			{
				break;
			}
			preceding.add(candidate);
		}
		PPose accumulated = evaluate(model, preceding, contexts, 1.0f);
		MolangParser.Context supplied = contexts.context(controller, 1.0f);
		MolangParser.Context context = controller.persistentMolangContext();
		if (supplied != context)
		{
			context.copyFrameValuesFrom(supplied);
		}
		return controller.graphRuntime().sampleBasePose(model, 1.0f, context, accumulated);
	}

	public static PRootMotionDelta extractRootMotion(PAnimation animation,
	                                                 String rootBoneName,
	                                                 float previousTime,
	                                                 float currentTime,
	                                                 PInterpolationType interpolation,
	                                                 Object data)
	{
		PBoneAnimation rootBone = animation.boneAnimations().get(rootBoneName);
		if (rootBone == null)
			return PRootMotionDelta.identity();

		BoneFrame previous = animation.calculateBoneTransformations(rootBone, previousTime, interpolation, data, null);
		BoneFrame current = animation.calculateBoneTransformations(rootBone, currentTime, interpolation, data, null);
		if (previous == null || current == null)
			return PRootMotionDelta.identity();

		Quaternionf previousRotation = new Quaternionf(previous.rotation());
		Quaternionf inversePreviousRotation = previousRotation.invert(new Quaternionf());
		Vector3f translation = new Vector3f(current.translation()).sub(previous.translation()).rotate(inversePreviousRotation);
		Quaternionf rotation = inversePreviousRotation.mul(current.rotation(), new Quaternionf()).normalize();
		return new PRootMotionDelta(translation, rotation);
	}

	private static <T extends PAnimatable<T>> @Nullable PCompiledAnimation animation(PBakedModel model, PAnimationController<T> controller)
	{
		PAnimationStagePlayback stage = controller.getCurrentStage();
		return stage == null || stage.isWaiting() ? null : model.compiledAnimation(stage.animationName());
	}

	static PPose sampleGraphLayers(PBakedModel model, List<PAnimationGraphRuntime.Layer> layers,
	                              MolangParser.Context context, PPose accumulatedPose)
	{
		PPose pose = new PPose(model.boneCount());
		for (int bone = 0; bone < model.boneCount(); bone++)
		{
			BoneFrame frame = calculateGraphBoneTransformations(model, layers, bone, context,
					new BoneFrame(new Vector3f(accumulatedPose.translation(bone)), new Quaternionf(accumulatedPose.rotation(bone)), new Vector3f(accumulatedPose.scale(bone))));
			boolean translation = false;
			boolean rotation = false;
			boolean scale = false;
			for (PAnimationGraphRuntime.Layer layer : layers)
			{
				PAnimation animation = model.animations().get(layer.animation());
				if (animation == null || layer.weight() <= 0.0f)
				{
					continue;
				}
				String boneName = model.bone(bone).name();
				if (animation.visibilityTracks().containsKey(boneName))
				{
					pose.visibility(bone, pose.isVisible(bone) && animation.isBoneVisible(boneName, layer.time()));
				}
				PBoneAnimation boneAnimation = animation.boneAnimations().get(boneName);
				if (boneAnimation != null)
				{
					translation |= hasKeys(boneAnimation, PLibRegistration.AnimationChannelReg.POSITION);
					rotation |= hasKeys(boneAnimation, PLibRegistration.AnimationChannelReg.ROTATION);
					scale |= hasKeys(boneAnimation, PLibRegistration.AnimationChannelReg.SCALE);
				}
			}
			if (frame != null)
			{
				if (translation)
				{
					pose.translation(bone, frame.translation());
				}
				if (rotation)
				{
					pose.rotation(bone, frame.rotation());
				}
				if (scale)
				{
					pose.scale(bone, frame.scale());
				}
			}
		}
		return pose;
	}

	static void applyGraphOverlay(PPose base, PPose delta, float weight)
	{
		for (int bone = 0; bone < base.boneCount(); bone++)
		{
			if (delta.hasTranslation(bone))
			{
				base.translation(bone, new Vector3f(base.translation(bone)).fma(weight, delta.translation(bone)));
			}
			if (delta.hasRotation(bone))
			{
				base.rotation(bone, new Quaternionf(base.rotation(bone)).premul(new Quaternionf().slerp(delta.rotation(bone), weight)).normalize());
			}
			if (delta.hasScale(bone))
			{
				base.scale(bone, new Vector3f(base.scale(bone)).mul(new Vector3f(1.0f).lerp(delta.scale(bone), weight)));
			}
			if (delta.hasVisibility(bone))
			{
				base.visibility(bone, base.isVisible(bone) && delta.isVisible(bone));
			}
		}
	}

	private static boolean hasKeys(PBoneAnimation animation, PAnimationChannelType<?> channel)
	{
		PAnimationTrack<?> track = animation.track(channel);
		return track != null && !track.keyframes().isEmpty();
	}

	private static @Nullable BoneFrame calculateGraphBoneTransformations(PBakedModel model,
	                                                                    List<PAnimationGraphRuntime.Layer> layers,
	                                                                    int boneIndex,
	                                                                    MolangParser.Context context,
	                                                                    BoneFrame accumulatedFrame)
	{
		List<GraphFrame> frames = new ArrayList<>(layers.size());
		for (PAnimationGraphRuntime.Layer layer : layers)
		{
			PCompiledAnimation animation = model.compiledAnimation(layer.animation());
			if (animation == null || animation.boneAnimation(boneIndex) == null)
				continue;
			BoneFrame frame = animation.animation().calculateBoneTransformations(
					animation.boneAnimation(boneIndex), layer.time(), layer.interpolation(),
					context, accumulatedFrame);
			if (frame != null)
				frames.add(new GraphFrame(layer, frame));
		}
		if (frames.isEmpty())
			return null;

		Vector3f translation = new Vector3f();
		Vector3f scale = new Vector3f(1f);
		Quaternionf anchor = null;
		float qx = 0.0f;
		float qy = 0.0f;
		float qz = 0.0f;
		float qw = 0.0f;
		float baseWeight = 0.0f;
		for (GraphFrame graphFrame : frames)
		{
			if (graphFrame.layer.overlay())
				continue;
			float weight = graphFrame.layer.weight();
			BoneFrame frame = graphFrame.frame;
			translation.fma(weight, frame.translation());
			scale.fma(weight, new Vector3f(frame.scale()).sub(1.0f, 1.0f, 1.0f));
			Quaternionf rotation = frame.rotation();
			if (anchor == null)
				anchor = rotation;
			float sign = anchor.dot(rotation) < 0.0f ? -1.0f : 1.0f;
			qx += rotation.x * weight * sign;
			qy += rotation.y * weight * sign;
			qz += rotation.z * weight * sign;
			qw += rotation.w * weight * sign;
			baseWeight += weight;
		}
		if (anchor == null)
			anchor = new Quaternionf();
		float identitySign = anchor.w < 0.0f ? -1.0f : 1.0f;
		qw += Math.max(0.0f, 1.0f - baseWeight) * identitySign;
		Quaternionf rotation = new Quaternionf(qx, qy, qz, qw);
		if (rotation.lengthSquared() < 1.0e-10f)
			rotation.identity();
		else
			rotation.normalize();

		for (GraphFrame graphFrame : frames)
		{
			if (!graphFrame.layer.overlay())
				continue;
			float weight = graphFrame.layer.weight();
			BoneFrame frame = graphFrame.frame;
			translation.fma(weight, frame.translation());
			scale.mul(new Vector3f(1f).lerp(frame.scale(), weight));
			rotation.premul(new Quaternionf().slerp(frame.rotation(), weight)).normalize();
		}
		return new BoneFrame(translation, rotation, scale);
	}

	private record GraphFrame(PAnimationGraphRuntime.Layer layer, BoneFrame frame)
	{
	}

	private static boolean differsFromBind(PBakedModel model,
	                                      int boneIndex,
	                                      Vector3f translation,
	                                      Quaternionf rotation,
	                                      Vector3f scale)
	{
		PBakedBone bone = model.bone(boneIndex);
		return translation.distanceSquared(bone.basePosition()) > 1.0e-10f ||
				Math.abs(Math.abs(rotation.dot(bone.baseRotation())) - 1f) > 1.0e-6f ||
				Math.abs(scale.x - 1f) > 1.0e-6f || Math.abs(scale.y - 1f) > 1.0e-6f || Math.abs(scale.z - 1f) > 1.0e-6f;
	}
}
