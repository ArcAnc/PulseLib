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

/**
 * Provides support for player animation frame.
 */
public final class PPlayerAnimationFrame
{
	private final PPlayerAnimationDefinition definition;
	private final PAnimationPoseResolver<PPlayerAnimationInstance> resolver;
	
	@Nullable
	private final String rootBone;
	private final Map<String, PTransform> fullTransforms = new HashMap<>();
	private final Set<String> missingFullTransforms = new HashSet<>();
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param definition the definition to use.
	 * @param resolver the resolver to use.
	 */
	PPlayerAnimationFrame(PPlayerAnimationDefinition definition,
	                             PAnimationPoseResolver<PPlayerAnimationInstance> resolver)
	{
		this.definition = Objects.requireNonNull(definition);
		this.resolver = Objects.requireNonNull(resolver);
		this.rootBone = definition.bindings().get(PPlayerPart.ROOT);
	}
	
	/**
	 * Performs the definition operation.
	 * @return the value produced by this operation.
	 */
	public PPlayerAnimationDefinition definition()
	{
		return this.definition;
	}
	
	/**
	 * Performs the resolver operation.
	 * @return the value produced by this operation.
	 */
	@ApiStatus.Internal
	public PAnimationPoseResolver<PPlayerAnimationInstance> resolver()
	{
		return this.resolver;
	}

	/**
	 * Performs the local transform operation.
	 * @param boneName the bone name to use.
	 * @return the value produced by this operation.
	 */
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
	
	/**
	 * Performs the model transform operation.
	 * @param boneName the bone name to use.
	 * @return the value produced by this operation.
	 */
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

	/**
	 * Performs the root relative transform operation.
	 * @param boneName the bone name to use.
	 * @return the value produced by this operation.
	 */
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
	
	/**
	 * Performs the full transform operation.
	 * @param boneName the bone name to use.
	 * @return the value produced by this operation.
	 */
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
	
	/**
	 * Performs the action transform operation.
	 * @param anchor the anchor to use.
	 * @return the value produced by this operation.
	 */
	@Nullable
	public PTransform actionTransform(PPlayerAnimationAnchor anchor)
	{
		String boneName = this.definition.anchors().get(anchor);
		return boneName == null ? null : modelTransform(boneName);
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

	/**
	 * Binds the transform.
	 * @param boneName the bone name to use.
	 * @return the value produced by this operation.
	 */
	@Nullable
	public PTransform bindTransform(String boneName)
	{
		PAnimationPoseResolver.BonePose pose =
				this.resolver.resolve(boneName);
		
		return pose == null ?
				null :
			pose.bindTransform();
	}
	
	/**
	 * Performs the ratio operation.
	 * @param value the value to use.
	 * @param base the base to use.
	 * @return the value produced by this operation.
	 */
	private static float ratio(float value, float base)
	{
		return Math.abs(base) < 1.0e-6f ? value : value / base;
	}
	
}
