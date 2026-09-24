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
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.model.baked.PBakedBone;
import com.arcanc.pulselib.data.gecko.MolangParser;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

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
		PPose pose = model.bindPose();
		BitSet affectedBones = new BitSet(model.boneCount());
		for (PAnimationController<T> controller : controllers)
		{
			List<PAnimationGraphRuntime.Layer> layers = controller.graphLayers(model);
			if (layers.isEmpty())
			{
				PCompiledAnimation animation = animation(model, controller);
				if (animation != null)
					affectedBones.or(animation.boneMask());
			}
			else
				for (PAnimationGraphRuntime.Layer layer : layers)
				{
					PCompiledAnimation animation = model.compiledAnimation(layer.animation());
					if (animation != null)
						affectedBones.or(animation.boneMask());
				}
		}

		for (int boneIndex = affectedBones.nextSetBit(0); boneIndex >= 0; boneIndex = affectedBones.nextSetBit(boneIndex + 1))
		{
			Vector3f translation = new Vector3f(pose.translation(boneIndex));
			Quaternionf rotation = new Quaternionf(pose.rotation(boneIndex));
			Vector3f scale = new Vector3f(pose.scale(boneIndex));
			boolean changed = false;
			for (PAnimationController<T> controller : controllers)
			{
				List<PAnimationGraphRuntime.Layer> layers = controller.graphLayers(model);
				BoneFrame frame;
				if (!layers.isEmpty())
					frame = calculateGraphBoneTransformations(model, controller, layers, boneIndex, contexts.context(controller, partialTick),
							new BoneFrame(new Vector3f(translation), new Quaternionf(rotation), new Vector3f(scale)));
				else
				{
					PCompiledAnimation animation = animation(model, controller);
					if (animation == null)
						continue;
					PBoneAnimation boneAnimation = animation.boneAnimation(boneIndex);
					if (boneAnimation == null)
						continue;
					frame = controller.calculateBoneTransformations(
							animation,
							boneIndex,
							partialTick,
							contexts.context(controller, partialTick),
							new BoneFrame(new Vector3f(translation), new Quaternionf(rotation), new Vector3f(scale)));
				}
				if (frame == null)
					continue;
				translation.add(frame.translation());
				rotation.premul(frame.rotation());
				scale.mul(frame.scale());
				changed = true;
			}
			if (changed && differsFromBind(model, boneIndex, translation, rotation, scale))
				pose.setAnimated(boneIndex, translation, rotation, scale);
		}
		return pose;
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

	private static <T extends PAnimatable<T>> PCompiledAnimation animation(PBakedModel model, PAnimationController<T> controller)
	{
		PAnimationStagePlayback stage = controller.getCurrentStage();
		return stage == null || stage.isWaiting() ? null : model.compiledAnimation(stage.animationName());
	}

	private static <T extends PAnimatable<T>> BoneFrame calculateGraphBoneTransformations(PBakedModel model,
	                                                                                      PAnimationController<T> controller,
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
					controller.persistentMolangContext().copyFrameValuesFrom(context), accumulatedFrame);
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
