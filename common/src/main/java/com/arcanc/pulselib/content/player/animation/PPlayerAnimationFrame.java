/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * One canonical evaluated player-animation pose.  Consumers use this shared
 * frame rather than independently sampling animation channels.
 */
public final class PPlayerAnimationFrame
{
	private final PPlayerAnimationDefinition definition;
	private final PAnimationPoseResolver<PPlayerAnimationInstance> resolver;
	private final @Nullable String rootBone;
	private final Map<String, Matrix4f> modelMatrices = new HashMap<>();
	private final Map<String, Matrix4f> bindMatrices = new HashMap<>();
	private final Map<String, PTransform> transforms = new HashMap<>();
	private final Set<String> missingTransforms = new HashSet<>();

	PPlayerAnimationFrame(PPlayerAnimationDefinition definition,
	                      PAnimationPoseResolver<PPlayerAnimationInstance> resolver)
	{
		this.definition = definition;
		this.resolver = resolver;
		this.rootBone = definition.bindings().get(PPlayerPart.ROOT);
	}

	public PPlayerAnimationDefinition definition()
	{
		return this.definition;
	}

	public PAnimationPoseResolver<PPlayerAnimationInstance> resolver()
	{
		return this.resolver;
	}

	public @Nullable BoneFrame localTransform(String boneName)
	{
		PAnimationPoseResolver.BonePose pose = this.resolver.resolve(boneName);
		return pose == null ? null : pose.localTransform();
	}

	public @Nullable PPlayerBonePose canonicalModelDelta(String boneName)
	{
		PTransform current = transform(boneName);
		PTransform bind = bindTransform(boneName);
		if (current == null || bind == null)
			return null;
		if (this.rootBone != null && !boneName.equals(this.rootBone))
		{
			PTransform rootCurrent = transform(this.rootBone);
			PTransform rootBind = bindTransform(this.rootBone);
			if (rootCurrent == null || rootBind == null)
				return null;
			Matrix4f currentMatrix = new Matrix4f();
			Matrix4f bindMatrix = new Matrix4f();
			Matrix4f rootMatrix = new Matrix4f();
			if (!modelMatrix(boneName, currentMatrix) || !bindMatrix(boneName, bindMatrix) ||
					!modelMatrix(this.rootBone, rootMatrix))
			{
				return null;
			}
			current = PTransform.fromMatrix(rootMatrix.invert().mul(currentMatrix));
			if (!bindMatrix(this.rootBone, rootMatrix))
			{
				return null;
			}
			bind = PTransform.fromMatrix(rootMatrix.invert().mul(bindMatrix));
		}
		Vector3f translation = current.translation().sub(bind.translation());
		Quaternionf rotation = new Quaternionf(bind.rotation()).invert().premul(current.rotation());
		Vector3f scale = new Vector3f(ratio(current.scale().x, bind.scale().x),
				ratio(current.scale().y, bind.scale().y), ratio(current.scale().z, bind.scale().z));
		boolean translated = translation.lengthSquared() > 1.0e-10f;
		boolean rotated = Math.abs(current.rotation().dot(bind.rotation())) < 0.999999f;
		boolean scaled = Math.abs(scale.x - 1f) > 1.0e-5f || Math.abs(scale.y - 1f) > 1.0e-5f || Math.abs(scale.z - 1f) > 1.0e-5f;
		return translated || rotated || scaled ? new PPlayerBonePose(translation, rotation, scale, translated, rotated, scaled) : null;
	}

	public @Nullable PTransform modelTransform(String boneName)
	{
		return transform(boneName);
	}

	/** Writes the exact parent matrix product, including inherited nonuniform scale. */
	public boolean modelMatrix(String boneName, Matrix4f destination)
	{
		return hierarchyMatrix(boneName, destination, false);
	}

	/** Writes the exact bind hierarchy in the same source-model basis. */
	public boolean bindMatrix(String boneName, Matrix4f destination)
	{
		return hierarchyMatrix(boneName, destination, true);
	}

	private boolean hierarchyMatrix(String boneName, Matrix4f destination, boolean bind)
	{
		Map<String, Matrix4f> matrices = bind ? this.bindMatrices : this.modelMatrices;
		Matrix4f cached = matrices.get(boneName);
		if (cached != null)
		{
			destination.set(cached);
			return true;
		}
		PBakedModel model = this.definition.modelData().getModel();
		if (model == null || model.boneIndex(boneName) < 0)
		{
			return false;
		}
		int index = model.boneIndex(boneName);
		int parent = model.parentIndex(index);
		destination.identity();
		if (parent >= 0 && !hierarchyMatrix(model.bone(parent).name(), destination, bind))
		{
			return false;
		}
		if (bind)
		{
			destination.translate(model.bone(index).basePosition()).rotate(model.bone(index).baseRotation());
		}
		else
		{
			BoneFrame local = localTransform(boneName);
			if (local == null)
			{
				return false;
			}
			destination.translate(local.translation()).rotate(local.rotation()).scale(local.scale());
		}
		matrices.put(boneName, new Matrix4f(destination));
		return true;
	}

	public @Nullable PTransform bindTransform(String boneName)
	{
		PAnimationPoseResolver.BonePose pose = this.resolver.resolve(boneName);
		return pose == null ? null : pose.bindTransform();
	}

	/**
	 * Returns the current MODEL transform in the ROOT bind frame (or HEAD bind
	 * frame for rootless rigs). This includes animated ROOT motion and is for
	 * callers whose outer stack has not already applied that motion.
	 */
	public @Nullable PTransform rootRelativeTransform(String boneName)
	{
		Matrix4f matrix = new Matrix4f();
		if (!modelMatrix(boneName, matrix))
		{
			return null;
		}
		String reference = this.rootBone == null ? this.definition.bindings().get(PPlayerPart.HEAD) : this.rootBone;
		Matrix4f bind = new Matrix4f();
		if (reference != null && bindMatrix(reference, bind))
		{
			bind.invert().mul(matrix, matrix);
		}
		return PTransform.fromMatrix(matrix);
	}

	/**
	 * Returns a bone in the current ROOT frame for a PoseStack that already
	 * contains visual ROOT motion. Without ROOT, the head bind frame remains
	 * the reference used by existing rootless player rigs.
	 */
	public @Nullable PTransform currentRootRelativeTransform(String boneName)
	{
		Matrix4f matrix = new Matrix4f();
		return currentRootRelativeMatrix(boneName, matrix) ? PTransform.fromMatrix(matrix) : null;
	}

	/** Exact rendering form of {@link #currentRootRelativeTransform(String)}. */
	public boolean currentRootRelativeMatrix(String boneName, Matrix4f destination)
	{
		if (!modelMatrix(boneName, destination))
		{
			return false;
		}
		String reference = this.rootBone == null ? this.definition.bindings().get(PPlayerPart.HEAD) : this.rootBone;
		if (reference == null)
		{
			return true;
		}
		Matrix4f root = new Matrix4f();
		boolean resolved = this.rootBone == null ? bindMatrix(reference, root) : modelMatrix(reference, root);
		if (resolved)
		{
			root.invert().mul(destination, destination);
		}
		return true;
	}

	/** Returns the full MODEL-space socket, including ROOT and every parent. */
	public @Nullable PTransform actionTransform(PPlayerAnimationAnchor anchor)
	{
		String bone = this.definition.anchors().get(anchor);
		return bone == null ? null : transform(bone);
	}

	/** Returns the animated first-person camera anchor relative to its bind pose. */
	public @Nullable PTransform cameraAnchorModelDelta()
	{
		String cameraBone = this.definition.anchors().get(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA);
		if (cameraBone == null) return null;
		PTransform current = transform(cameraBone);
		PTransform bind = bindTransform(cameraBone);
		return current == null || bind == null ? null : current.compose(bind.inverse());
	}

	private @Nullable PTransform transform(String boneName)
	{
		PTransform cached = this.transforms.get(boneName);
		if (cached != null) return cached;
		if (this.missingTransforms.contains(boneName)) return null;
		PAnimationPoseResolver.BonePose pose = this.resolver.resolve(boneName);
		if (pose == null)
		{
			this.missingTransforms.add(boneName);
			return null;
		}
		PTransform transform = pose.modelTransform();
		Matrix4f matrix = new Matrix4f();
		if (modelMatrix(boneName, matrix))
		{
			transform = new PTransform(matrix.getTranslation(new Vector3f()), transform.rotation(), transform.scale());
		}
		this.transforms.put(boneName, transform);
		return transform;
	}

	private static float ratio(float value, float base)
	{
		return Math.abs(base) < 1.0e-6f ? value : value / base;
	}
}
