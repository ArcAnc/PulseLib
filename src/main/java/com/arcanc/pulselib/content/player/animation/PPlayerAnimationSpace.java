package com.arcanc.pulselib.content.player.animation;

import com.arcanc.pulselib.content.model.animation.PTransform;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Coordinate conversion between imported player models and vanilla ModelPart space. */
public final class PPlayerAnimationSpace
{
	private PPlayerAnimationSpace() {}

	public static Vector3f toPlayerSpace(Vector3fc vector, PPlayerAnimationDefinition definition)
	{
		Vector3f result = new Vector3f(vector);
		return usesGltfCoordinates(definition) ? result.mul(-1f, -1f, 1f) : result;
	}

	public static Quaternionf toPlayerSpace(Quaternionfc rotation, PPlayerAnimationDefinition definition)
	{
		Quaternionf result = new Quaternionf(rotation);
		return usesGltfCoordinates(definition) ? result.set(-result.x, -result.y, result.z, result.w) : result;
	}

	public static PPlayerBonePose toPlayerSpace(PPlayerBonePose pose, PPlayerAnimationDefinition definition)
	{
		return new PPlayerBonePose(toPlayerSpace(pose.translation(), definition), toPlayerSpace(pose.rotation(), definition),
				pose.scale(), pose.hasTranslation(), pose.hasRotation(), pose.hasScale());
	}

	public static PTransform toPlayerSpace(PTransform transform, PPlayerAnimationDefinition definition)
	{
		return usesGltfCoordinates(definition) ? new PTransform(toPlayerSpace(transform.translation(), definition),
				toPlayerSpace(transform.rotation(), definition), transform.scale()) : transform;
	}

	private static boolean usesGltfCoordinates(PPlayerAnimationDefinition definition)
	{
		return definition.modelData().getModelFormat().equals(PGltfModelLoader.INSTANCE.id());
	}
}
