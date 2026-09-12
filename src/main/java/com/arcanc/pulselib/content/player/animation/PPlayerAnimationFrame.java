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
	private final Map<String, PTransform> firstPersonTransforms = new HashMap<>();
	private final Set<String> missingFirstPersonTransforms = new HashSet<>();
	@Nullable
	private PTransform firstPersonCameraInverse;
	@Nullable
	private PTransform firstPersonBindCameraInverse;
	private boolean firstPersonCameraResolved;
	
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
	
	public @Nullable PPlayerBonePose animationDelta(String boneName)
	{
		PAnimationPoseResolver.AnimationDelta pose =
				this.resolver.animationDelta(
						boneName,
						this.rootBone
				);
		
		if (pose == null || !pose.isAnimated())
			return null;
		
		return new PPlayerBonePose(
				pose.translation(),
				pose.rotation(),
				pose.scale(),
				pose.hasTranslation(),
				pose.hasRotation(),
				pose.hasScale()
		);
	}
	
	public @Nullable PPlayerBonePose animationDelta(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		
		return boneName == null ?
				null :
				animationDelta(boneName);
	}
	
	@Nullable
	public PTransform modelTransform(String boneName)
	{
		return fullTransform(boneName);
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
	
	@Nullable
	public PTransform actionTransform(PPlayerAnimationAnchor anchor)
	{
		return modelTransform(anchor);
	}
	
	@Nullable
	public PTransform firstPersonTransform(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		return boneName == null ? null : copyFirstPersonTransform(boneName);
	}
	
	@Nullable
	public PTransform firstPersonTransform(PPlayerAnimationAnchor anchor)
	{
		String boneName = this.definition.anchors().get(anchor);
		return boneName == null ? null : copyFirstPersonTransform(boneName);
	}

	@Nullable
	public PTransform firstPersonTransform(String boneName)
	{
		return copyFirstPersonTransform(boneName);
	}

	/** Current transform of a semantic socket relative to FIRST_PERSON_CAMERA. */
	@Nullable
	public PTransform firstPersonSocketTransform(PPlayerAnimationAnchor socket)
	{
		return firstPersonTransform(socket);
	}

	/** Bind transform of a semantic socket relative to FIRST_PERSON_CAMERA. */
	@Nullable
	public PTransform firstPersonBindSocketTransform(PPlayerAnimationAnchor socket)
	{
		String boneName = this.definition.anchors().get(socket);
		if (boneName == null)
			return null;
		PTransform cameraInverse = firstPersonBindCameraInverse();
		PTransform bone = bindTransform(boneName);
		return cameraInverse == null || bone == null ? null : cameraInverse.compose(bone);
	}

	@Nullable
	public PTransform firstPersonBindTransform(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		PTransform cameraInverse = firstPersonBindCameraInverse();
		PTransform bone = boneName == null ? null : bindTransform(boneName);
		return cameraInverse == null || bone == null ? null : cameraInverse.compose(bone);
	}

	/**
	 * Returns {@code currentSocket * bindSocket^-1}. In the rest frame this is
	 * identity and is therefore independent from GLTF pivots and bind offsets.
	 */
	@Nullable
	public PTransform firstPersonSocketDelta(PPlayerAnimationAnchor socket)
	{
		PTransform current = firstPersonSocketTransform(socket);
		PTransform bind = firstPersonBindSocketTransform(socket);
		return current == null || bind == null ? null : current.compose(bind.inverse());
	}

	/** The FIRST_PERSON_CAMERA anchor delta in model space. */
	@Nullable
	public PTransform firstPersonCameraDelta()
	{
		String cameraBone = this.definition.anchors().get(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA);
		if (cameraBone == null)
			return null;
		PTransform current = fullTransform(cameraBone);
		PTransform bind = bindTransform(cameraBone);
		return current == null || bind == null ? null : current.compose(bind.inverse());
	}

	@Nullable
	private PTransform copyFirstPersonTransform(String boneName)
	{
		return cachedFirstPersonTransform(boneName);
	}

	@Nullable
	private PTransform cachedFirstPersonTransform(String boneName)
	{
		PTransform cached = this.firstPersonTransforms.get(boneName);
		if (cached != null)
			return cached;
		if (this.missingFirstPersonTransforms.contains(boneName))
			return null;

		PTransform cameraInverse = firstPersonCameraInverse();
		PTransform bone = fullTransform(boneName);
		if (cameraInverse == null || bone == null)
		{
			this.missingFirstPersonTransforms.add(boneName);
			return null;
		}

		PTransform transform = cameraInverse.compose(bone);
		this.firstPersonTransforms.put(boneName, transform);
		return transform;
	}

	@Nullable
	private PTransform firstPersonCameraInverse()
	{
		if (!this.firstPersonCameraResolved)
		{
			this.firstPersonCameraResolved = true;
			String cameraBone = this.definition.anchors().get(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA);
			PTransform camera = cameraBone == null ? null : fullTransform(cameraBone);
			if (camera != null)
				this.firstPersonCameraInverse = camera.inverse();
		}
		return this.firstPersonCameraInverse;
	}

	@Nullable
	private PTransform firstPersonBindCameraInverse()
	{
		if (!this.firstPersonCameraResolved)
			firstPersonCameraInverse();
		if (this.firstPersonBindCameraInverse == null)
		{
			String cameraBone = this.definition.anchors().get(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA);
			PTransform camera = cameraBone == null ? null : bindTransform(cameraBone);
			if (camera != null)
				this.firstPersonBindCameraInverse = camera.inverse();
		}
		return this.firstPersonBindCameraInverse;
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
	
	@Nullable
	public BoneFrame animationTransform(PPlayerPart part)
	{
		String boneName =
				this.definition.bindings().get(part);
		
		if (boneName == null)
			return null;
		
		PAnimationPoseResolver.BonePose pose =
				this.resolver.resolve(boneName);
		
		return pose == null ?
				null :
				pose.animationTransform();
	}
}
