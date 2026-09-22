package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.BoneFrame;
import com.arcanc.pulselib.content.model.animation.PAnimationPoseResolver;
import com.arcanc.pulselib.content.model.animation.PTransform;
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
	private final String rootBone;
	private final Map<String, PTransform> transforms = new HashMap<>();
	private final Set<String> missingTransforms = new HashSet<>();

	PPlayerAnimationFrame(PPlayerAnimationDefinition definition,
	                      PAnimationPoseResolver<PPlayerAnimationInstance> resolver)
	{
		this.definition = definition;
		this.resolver = resolver;
		this.rootBone = definition.bindings().get(PPlayerPart.ROOT);
	}

	public PPlayerAnimationDefinition definition() { return this.definition; }
	public PAnimationPoseResolver<PPlayerAnimationInstance> resolver() { return this.resolver; }

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
			current = rootCurrent.inverse().compose(current);
			bind = rootBind.inverse().compose(bind);
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

	public @Nullable PTransform modelTransform(String boneName) { return transform(boneName); }

	public boolean modelMatrix(String boneName, Matrix4f destination)
	{
		PTransform transform = transform(boneName);
		if (transform == null) return false;
		transform.matrix(destination);
		return true;
	}

	public @Nullable PTransform bindTransform(String boneName)
	{
		PAnimationPoseResolver.BonePose pose = this.resolver.resolve(boneName);
		return pose == null ? null : pose.bindTransform();
	}

	public @Nullable PTransform actionTransform(PPlayerAnimationAnchor anchor)
	{
		String bone = this.definition.anchors().get(anchor);
		return bone == null ? null : transform(bone);
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
		this.transforms.put(boneName, transform);
		return transform;
	}

	private static float ratio(float value, float base)
	{
		return Math.abs(base) < 1.0e-6f ? value : value / base;
	}
}
