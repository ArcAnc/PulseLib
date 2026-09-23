/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

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
	/** Converts glTF model space to the basis expected by vanilla player geometry. */
	private static final PTransform GLTF_TO_PLAYER_GEOMETRY =
			PTransform.rotation(new Quaternionf().rotationZ((float)Math.PI));

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

	/** Converts a model transform for geometry rendered by vanilla player parts. */
	public static PTransform toPlayerGeometrySpace(PTransform transform, PPlayerAnimationDefinition definition)
	{
		return usesGltfCoordinates(definition) ? GLTF_TO_PLAYER_GEOMETRY.compose(transform) : transform;
	}

	/**
	 * Converts a camera-relative transform for imported mesh geometry. glTF
	 * vertices remain in their source basis, so the two basis changes cancel.
	 */
	public static PTransform toFirstPersonGeometrySpace(PTransform transform, PPlayerAnimationDefinition definition)
	{
		return usesGltfCoordinates(definition) ? GLTF_TO_PLAYER_GEOMETRY.compose(toPlayerGeometrySpace(transform, definition)) : transform;
	}

	private static boolean usesGltfCoordinates(PPlayerAnimationDefinition definition)
	{
		return definition.modelData().getModelFormat().equals(PGltfModelLoader.INSTANCE.id());
	}
}
