/**
 * @author ArcAnc
 * Created at: 11.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Immutable translation, rotation, and scale transform data.
 *
 * @param translation the local translation vector
 * @param rotation the local quaternion rotation
 * @param scale the local scale vector
 */
public record PTransform(Vector3f translation, Quaternionf rotation, Vector3f scale)
{
	/**
	 * Transform with zero translation, identity rotation and unit scale.
	 */
	public static final PTransform IDENTITY = new PTransform(
			new Vector3f(), new Quaternionf(), new Vector3f(1.0f));

	/**
	 * Copies translation, rotation and scale into an independent transform value.
	 *
	 * @param translation the local translation vector
	 * @param rotation the local quaternion rotation
	 * @param scale the local scale vector
	 */
	public PTransform
	{
		translation = new Vector3f(Objects.requireNonNull(translation));
		rotation = new Quaternionf(Objects.requireNonNull(rotation));
		scale = new Vector3f(Objects.requireNonNull(scale));
	}

	/**
	 * Returns a defensive copy of the translation.
	 *
	 * @return a defensive copy of the translation
	 */
	@Override
	public Vector3f translation()
	{
		return new Vector3f(this.translation);
	}

	/**
	 * Returns a defensive copy of the rotation.
	 *
	 * @return a defensive copy of the rotation
	 */
	@Override
	public Quaternionf rotation()
	{
		return new Quaternionf(this.rotation);
	}

	/**
	 * Returns a defensive copy of the scale.
	 *
	 * @return a defensive copy of the scale
	 */
	@Override
	public Vector3f scale()
	{
		return new Vector3f(this.scale);
	}

	/**
	 * Materializes this transform at the rendering boundary.
	 *
	 * @return the four-by-four transform matrix
	 */
	public Matrix4f matrix()
	{
		return matrix(new Matrix4f());
	}

	/**
	 * Writes this LOCAL_BONE or MODEL-space TRS transform into {@code destination}.
	 * This overload is for render hot paths that already own a matrix scratch buffer.
	 *
	 * @param destination the mutable object receiving the result
	 * @return the four-by-four transform matrix
	 */
	public Matrix4f matrix(Matrix4f destination)
	{
		return destination.translationRotateScale(this.translation, this.rotation, this.scale);
	}

	/**
	 * Decomposes a matrix at a boundary where a transform must change bases.
	 * Animation transforms are TRS, therefore this deliberately does not try to
	 * preserve shearing matrices.
	 *
	 * @param matrix the matrix
	 * @return the transform decomposed from the supplied matrix
	 */
	public static PTransform fromMatrix(Matrix4f matrix)
	{
		Objects.requireNonNull(matrix);
		return new PTransform(
				matrix.getTranslation(new Vector3f()),
				matrix.getUnnormalizedRotation(new Quaternionf()).normalize(),
				matrix.getScale(new Vector3f()));
	}

	/**
	 * Interpolates translation, rotation and scale toward the target transform.
	 *
	 * @param target the target
	 * @param weight the contribution weight
	 * @return the interpolated translation, rotation and scale
	 */
	public PTransform interpolate(PTransform target, float weight)
	{
		Objects.requireNonNull(target);
		return new PTransform(
				new Vector3f(this.translation).lerp(target.translation, weight),
				new Quaternionf(this.rotation).slerp(target.rotation, weight),
				new Vector3f(this.scale).lerp(target.scale, weight));
	}

	/**
	 * Applies {@code local} after this transform using TRS channel semantics.
	 *
	 * @param local the local
	 * @return the composed parent and local transform
	 */
	public PTransform compose(PTransform local)
	{
		Objects.requireNonNull(local);
		return new PTransform(
				new Vector3f(local.translation).mul(this.scale).rotate(this.rotation).add(this.translation),
				new Quaternionf(this.rotation).mul(local.rotation),
				new Vector3f(this.scale).mul(local.scale));
	}

	/**
	 * Returns the inverse TRS channels used by relative player-animation transforms.
	 *
	 * @return the inverse TRS channels used by relative player-animation transforms
	 */
	public PTransform inverse()
	{
		Quaternionf inverseRotation = new Quaternionf(this.rotation).invert();
		Vector3f inverseScale = new Vector3f(
				safeInverse(this.scale.x), safeInverse(this.scale.y), safeInverse(this.scale.z));
		return new PTransform(
				new Vector3f(this.translation).negate().mul(inverseScale).rotate(inverseRotation),
				inverseRotation,
				inverseScale);
	}

	/**
	 * Returns a transform copy with the supplied translation.
	 *
	 * @param translation the local translation vector
	 * @return a transform copy with the supplied translation
	 */
	public static PTransform translation(Vector3f translation)
	{
		return new PTransform(translation, new Quaternionf(), new Vector3f(1.0f));
	}

	/**
	 * Returns a transform copy with the supplied rotation.
	 *
	 * @param rotation the local quaternion rotation
	 * @return a transform copy with the supplied rotation
	 */
	public static PTransform rotation(Quaternionf rotation)
	{
		return new PTransform(new Vector3f(), rotation, new Vector3f(1.0f));
	}

	/**
	 * Returns the reciprocal of a scale component with zero handled safely.
	 *
	 * @param value the value written by this operation
	 * @return the reciprocal of a scale component with zero handled safely
	 */
	private static float safeInverse(float value)
	{
		return Math.abs(value) < 1.0e-6f ? 0.0f : 1.0f / value;
	}
}
