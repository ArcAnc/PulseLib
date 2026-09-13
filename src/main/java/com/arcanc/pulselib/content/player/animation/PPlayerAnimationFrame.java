/**
 * @author ArcAnc
 * Created at: 04.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;


import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.PTransform;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class PPlayerAnimationFrame
{
	private final PPlayerAnimationDefinition definition;
	private final PAnimationPoseResolver<PPlayerAnimationInstance> resolver;
	
	@Nullable
	private final String rootBone;
	private final Map<String, PTransform> fullTransforms = new HashMap<>();
	private final Set<String> missingFullTransforms = new HashSet<>();
	
	PPlayerAnimationFrame(PPlayerAnimationDefinition definition,
	                             PAnimationPoseResolver<PPlayerAnimationInstance> resolver)
	{
		this.definition = Objects.requireNonNull(definition);
		this.resolver = Objects.requireNonNull(resolver);
		this.rootBone = definition.bindings().get(PPlayerPart.ROOT);
	}
	
	public PPlayerAnimationDefinition definition()
	{
		return this.definition;
	}
	
	@ApiStatus.Internal
	public PAnimationPoseResolver<PPlayerAnimationInstance> resolver()
	{
		return this.resolver;
	}

	@Nullable
	public BoneFrame localTransform(String boneName)
	{
		PAnimationPoseResolver.BonePose pose = this.resolver.resolve(boneName);
		return pose == null ? null : pose.localTransform();
	}
	
	/**
	 * Delta for Minecraft's third-person ModelPart bridge, derived from the same
	 * canonical MODEL-space pose exposed by {@link #modelMatrix(String, Matrix4f)}.
	 */
	public @Nullable PPlayerBonePose canonicalModelDelta(String boneName)
	{
		PTransform current = fullTransform(boneName);
		PTransform bind = bindTransform(boneName);
		if (current == null || bind == null)
			return null;

		if (this.rootBone != null && !boneName.equals(this.rootBone))
		{
			PTransform rootCurrent = fullTransform(this.rootBone);
			PTransform rootBind = bindTransform(this.rootBone);
			if (rootCurrent == null || rootBind == null)
				return null;
			current = rootCurrent.inverse().compose(current);
			bind = rootBind.inverse().compose(bind);
		}

		Vector3f translation = current.translation().sub(bind.translation());
		Quaternionf currentRotation = current.rotation();
		Quaternionf bindRotation = bind.rotation();
		Quaternionf rotation = new Quaternionf(bindRotation).invert().premul(currentRotation);
		Vector3f currentScale = current.scale();
		Vector3f bindScale = bind.scale();
		Vector3f scale = new Vector3f(
				ratio(currentScale.x, bindScale.x),
				ratio(currentScale.y, bindScale.y),
				ratio(currentScale.z, bindScale.z));
		boolean hasTranslation = translation.lengthSquared() > 1.0e-10f;
		boolean hasRotation = Math.abs(currentRotation.dot(bindRotation)) < 0.999999f;
		boolean hasScale = Math.abs(scale.x - 1.0f) > 1.0e-5f ||
				Math.abs(scale.y - 1.0f) > 1.0e-5f ||
				Math.abs(scale.z - 1.0f) > 1.0e-5f;
		if (!hasTranslation && !hasRotation && !hasScale)
			return null;
		
		return new PPlayerBonePose(
				translation,
				rotation,
				scale,
				hasTranslation,
				hasRotation,
				hasScale
		);
	}
	
	public @Nullable PPlayerBonePose canonicalModelDelta(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		
		return boneName == null ?
				null :
			canonicalModelDelta(boneName);
	}
	
	@Nullable
	public PTransform modelTransform(String boneName)
	{
		return fullTransform(boneName);
	}

	/**
	 * Writes this bone's resolved canonical MODEL-space matrix.  It includes the
	 * evaluated local TRS, every parent bone and the imported model hierarchy.
	 * Presentation code must consume this matrix rather than reinterpreting
	 * animation channels.
	 */
	public boolean modelMatrix(String boneName, Matrix4f destination)
	{
		Objects.requireNonNull(destination);
		PTransform transform = fullTransform(boneName);
		if (transform == null)
			return false;
		transform.matrix(destination);
		return true;
	}

	/** Writes the bind-pose MODEL-space matrix for a bone. */
	public boolean bindModelMatrix(String boneName, Matrix4f destination)
	{
		Objects.requireNonNull(destination);
		PTransform transform = bindTransform(boneName);
		if (transform == null)
			return false;
		transform.matrix(destination);
		return true;
	}

	@Nullable
	public PTransform rootRelativeTransform(String boneName)
	{
		PTransform transform = fullTransform(boneName);
		if (transform == null)
			return null;

		String referenceBone = this.rootBone == null ?
				this.definition.bindings().get(PPlayerPart.HEAD) :
				this.rootBone;
		if (referenceBone == null)
			return transform;

		PTransform rootBind = bindTransform(referenceBone);
		return rootBind == null ? transform : rootBind.inverse().compose(transform);
	}
	
	@Nullable
	private PTransform fullTransform(String boneName)
	{
		PTransform cached = this.fullTransforms.get(boneName);
		if (cached != null)
			return cached;
		if (this.missingFullTransforms.contains(boneName))
			return null;

		PAnimationPoseResolver.BonePose pose =
				this.resolver.resolve(boneName);
		if (pose == null)
		{
			this.missingFullTransforms.add(boneName);
			return null;
		}

		PTransform transform = pose.modelTransform();
		this.fullTransforms.put(boneName, transform);
		return transform;
	}
	
	@Nullable
	public PTransform modelTransform(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		
		return boneName == null ?
				null :
				modelTransform(boneName);
	}

	public boolean modelMatrix(PPlayerPart part, Matrix4f destination)
	{
		String boneName = this.definition.bindings().get(part);
		return boneName != null && modelMatrix(boneName, destination);
	}
	
	@Nullable
	public PTransform modelTransform(
			PPlayerAnimationAnchor anchor)
	{
		String boneName =
				this.definition.anchors().get(anchor);
		
		return boneName == null ?
				null :
				modelTransform(boneName);
	}

	public boolean modelMatrix(PPlayerAnimationAnchor anchor, Matrix4f destination)
	{
		String boneName = this.definition.anchors().get(anchor);
		return boneName != null && modelMatrix(boneName, destination);
	}
	
	@Nullable
	public PTransform actionTransform(PPlayerAnimationAnchor anchor)
	{
		return modelTransform(anchor);
	}
	
	/** The FIRST_PERSON_CAMERA anchor delta in canonical MODEL space. */
	@Nullable
	public PTransform cameraAnchorModelDelta()
	{
		String cameraBone = this.definition.anchors().get(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA);
		if (cameraBone == null)
			return null;
		PTransform current = fullTransform(cameraBone);
		PTransform bind = bindTransform(cameraBone);
		return current == null || bind == null ? null : current.compose(bind.inverse());
	}

	@Nullable
	public PTransform bindTransform(String boneName)
	{
		PAnimationPoseResolver.BonePose pose =
				this.resolver.resolve(boneName);
		
		return pose == null ?
				null :
			pose.bindTransform();
	}
	
	@Nullable
	public PTransform bindTransform(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		
		return boneName == null ?
				null :
				bindTransform(boneName);
	}
	
	@Nullable
	public PTransform relativeTransform(
			String boneName,
			String referenceBoneName)
	{
		PTransform bone = fullTransform(boneName);
		PTransform reference = fullTransform(referenceBoneName);
		
		if (bone == null || reference == null)
			return null;
		
		return reference.inverse().compose(bone);
	}

	private static float ratio(float value, float base)
	{
		return Math.abs(base) < 1.0e-6f ? value : value / base;
	}
	
	@Nullable
	public PTransform relativeTransform(
			PPlayerAnimationAnchor bone,
			PPlayerAnimationAnchor reference)
	{
		String boneName =
				this.definition.anchors().get(bone);
		
		String referenceName =
				this.definition.anchors().get(reference);
		
		if (boneName == null || referenceName == null)
			return null;
		
		return relativeTransform(
				boneName,
				referenceName
		);
	}
	
	@Nullable
	public PTransform relativeTransform(
			PPlayerPart part,
			PPlayerAnimationAnchor reference)
	{
		String boneName =
				this.definition.bindings().get(part);
		
		String referenceName =
				this.definition.anchors().get(reference);
		
		if (boneName == null || referenceName == null)
			return null;
		
		return relativeTransform(
				boneName,
				referenceName
		);
	}
	
}
