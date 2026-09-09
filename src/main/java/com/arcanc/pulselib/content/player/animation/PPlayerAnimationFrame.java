/**
 * @author ArcAnc
 * Created at: 04.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation;


import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.BoneFrame;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PPlayerAnimationFrame
{
	private final PPlayerAnimationDefinition definition;
	private final PAnimationPoseResolver<PPlayerAnimationInstance> resolver;
	
	@Nullable
	private final String rootBone;
	private final Map<String, Matrix4f> fullTransforms = new HashMap<>();
	private final Set<String> missingFullTransforms = new HashSet<>();
	private final Map<String, Matrix4f> firstPersonTransforms = new HashMap<>();
	private final Set<String> missingFirstPersonTransforms = new HashSet<>();
	@Nullable
	private Matrix4f firstPersonCameraInverse;
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
	public Matrix4f modelTransform(String boneName)
	{
		Matrix4f transform = fullTransform(boneName);
		return transform == null ? null : new Matrix4f(transform);
	}
	
	@Nullable
	private Matrix4f fullTransform(String boneName)
	{
		Matrix4f cached = this.fullTransforms.get(boneName);
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

		Matrix4f transform = new Matrix4f(pose.modelTransform());
		this.fullTransforms.put(boneName, transform);
		return transform;
	}
	
	@Nullable
	public Matrix4f modelTransform(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		
		return boneName == null ?
				null :
				modelTransform(boneName);
	}
	
	@Nullable
	public Matrix4f modelTransform(
			PPlayerAnimationAnchor anchor)
	{
		String boneName =
				this.definition.anchors().get(anchor);
		
		return boneName == null ?
				null :
				modelTransform(boneName);
	}
	
	@Nullable
	public Matrix4f actionTransform(PPlayerAnimationAnchor anchor)
	{
		return modelTransform(anchor);
	}
	
	@Nullable
	public Matrix4f firstPersonTransform(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		return boneName == null ? null : copyFirstPersonTransform(boneName);
	}
	
	@Nullable
	public Matrix4f firstPersonTransform(PPlayerAnimationAnchor anchor)
	{
		String boneName = this.definition.anchors().get(anchor);
		return boneName == null ? null : copyFirstPersonTransform(boneName);
	}

	@Nullable
	public Matrix4f firstPersonTransform(String boneName)
	{
		return copyFirstPersonTransform(boneName);
	}

	@Nullable
	private Matrix4f copyFirstPersonTransform(String boneName)
	{
		Matrix4f transform = cachedFirstPersonTransform(boneName);
		return transform == null ? null : new Matrix4f(transform);
	}

	@Nullable
	private Matrix4f cachedFirstPersonTransform(String boneName)
	{
		Matrix4f cached = this.firstPersonTransforms.get(boneName);
		if (cached != null)
			return cached;
		if (this.missingFirstPersonTransforms.contains(boneName))
			return null;

		Matrix4f cameraInverse = firstPersonCameraInverse();
		Matrix4f bone = fullTransform(boneName);
		if (cameraInverse == null || bone == null)
		{
			this.missingFirstPersonTransforms.add(boneName);
			return null;
		}

		Matrix4f transform = new Matrix4f(cameraInverse).mul(bone);
		this.firstPersonTransforms.put(boneName, transform);
		return transform;
	}

	@Nullable
	private Matrix4f firstPersonCameraInverse()
	{
		if (!this.firstPersonCameraResolved)
		{
			this.firstPersonCameraResolved = true;
			String cameraBone = this.definition.anchors().get(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA);
			Matrix4f camera = cameraBone == null ? null : fullTransform(cameraBone);
			if (camera != null)
				this.firstPersonCameraInverse = new Matrix4f(camera).invert();
		}
		return this.firstPersonCameraInverse;
	}
	
	@Nullable
	public Matrix4f bindTransform(String boneName)
	{
		PAnimationPoseResolver.BonePose pose =
				this.resolver.resolve(boneName);
		
		return pose == null ?
				null :
				new Matrix4f(pose.bindTransform());
	}
	
	@Nullable
	public Matrix4f bindTransform(PPlayerPart part)
	{
		String boneName = this.definition.bindings().get(part);
		
		return boneName == null ?
				null :
				bindTransform(boneName);
	}
	
	@Nullable
	public Matrix4f relativeTransform(
			String boneName,
			String referenceBoneName)
	{
		Matrix4f bone = fullTransform(boneName);
		Matrix4f reference = fullTransform(referenceBoneName);
		
		if (bone == null || reference == null)
			return null;
		
		return new Matrix4f(reference).
				invert().
				mul(bone);
	}
	
	@Nullable
	public Matrix4f relativeTransform(
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
	public Matrix4f relativeTransform(
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
