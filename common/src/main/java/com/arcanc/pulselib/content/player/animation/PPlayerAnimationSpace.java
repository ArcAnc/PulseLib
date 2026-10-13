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
import org.joml.Matrix4f;
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

	/** The visual ROOT matrix in source axes; the native player and camera share this pivot convention. */
	public static Matrix4f visualRootMatrix(PPlayerBonePose pose, Vector3f pivot, float weight, Matrix4f destination)
	{
		Vector3f translation = pose.translation().mul(weight);
		Quaternionf rotation = new Quaternionf().slerp(pose.rotation(), weight);
		Vector3f scale = new Vector3f(1).lerp(pose.scale(), weight);
		return destination.identity().translate(translation).translate(pivot).rotate(rotation).scale(scale).
				translate(-pivot.x, -pivot.y, -pivot.z);
	}

	/** Changes both the transform input and output basis for native anchor geometry. */
	public static Matrix4f toPlayerSpace(Matrix4f matrix, PPlayerAnimationDefinition definition)
	{
		if (usesGltfCoordinates(definition))
		{
			matrix.scale(-1, -1, 1).scaleLocal(-1, -1, 1);
		}
		return matrix;
	}

	/** Changes the output basis while imported mesh vertices retain their source basis. */
	public static Matrix4f toPlayerGeometrySpace(Matrix4f matrix, PPlayerAnimationDefinition definition)
	{
		if (usesGltfCoordinates(definition))
		{
			matrix.scaleLocal(-1, -1, 1);
		}
		return matrix;
	}

	/** Preserves a caller's command edits while restoring the exact frame matrix underneath its TRS channels. */
	public static Matrix4f applyChannelOverride(Matrix4f exact, PTransform command, PTransform sampled)
	{
		Matrix4f authored = command.matrix();
		Matrix4f baseline = sampled.matrix();
		if (!authored.equals(baseline, 1.0e-5f))
		{
			authored.mul(baseline.invert()).mul(exact, exact);
		}
		return exact;
	}

	private static boolean usesGltfCoordinates(PPlayerAnimationDefinition definition)
	{
		return definition.modelData().getModelLoaderId().equals(PGltfModelLoader.INSTANCE.id());
	}
}
