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

/** Immutable translation, rotation, and scale transform data. */
public record PTransform(Vector3f translation, Quaternionf rotation, Vector3f scale)
{
	public static final PTransform IDENTITY = new PTransform(
			new Vector3f(), new Quaternionf(), new Vector3f(1.0f));

	public PTransform
	{
		translation = new Vector3f(Objects.requireNonNull(translation));
		rotation = new Quaternionf(Objects.requireNonNull(rotation));
		scale = new Vector3f(Objects.requireNonNull(scale));
	}

	@Override
	public Vector3f translation()
	{
		return new Vector3f(this.translation);
	}

	@Override
	public Quaternionf rotation()
	{
		return new Quaternionf(this.rotation);
	}

	@Override
	public Vector3f scale()
	{
		return new Vector3f(this.scale);
	}

	/** Materializes this transform at the rendering boundary. */
	public Matrix4f matrix()
	{
		return new Matrix4f().translationRotateScale(
				this.translation, this.rotation, this.scale);
	}

	/**
	 * Decomposes a matrix at a boundary where a transform must change bases.
	 * Animation transforms are TRS, therefore this deliberately does not try to
	 * preserve shearing matrices.
	 */
	public static PTransform fromMatrix(Matrix4f matrix)
	{
		Objects.requireNonNull(matrix);
		return new PTransform(
				matrix.getTranslation(new Vector3f()),
				matrix.getUnnormalizedRotation(new Quaternionf()).normalize(),
				matrix.getScale(new Vector3f()));
	}

	public PTransform interpolate(PTransform target, float weight)
	{
		Objects.requireNonNull(target);
		return new PTransform(
				new Vector3f(this.translation).lerp(target.translation, weight),
				new Quaternionf(this.rotation).slerp(target.rotation, weight),
				new Vector3f(this.scale).lerp(target.scale, weight));
	}

	/** Applies {@code local} after this transform using TRS channel semantics. */
	public PTransform compose(PTransform local)
	{
		Objects.requireNonNull(local);
		return new PTransform(
				new Vector3f(local.translation).mul(this.scale).rotate(this.rotation).add(this.translation),
				new Quaternionf(this.rotation).mul(local.rotation),
				new Vector3f(this.scale).mul(local.scale));
	}

	/** Returns the inverse TRS channels used by relative player-animation transforms. */
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

	public static PTransform translation(Vector3f translation)
	{
		return new PTransform(translation, new Quaternionf(), new Vector3f(1.0f));
	}

	public static PTransform rotation(Quaternionf rotation)
	{
		return new PTransform(new Vector3f(), rotation, new Vector3f(1.0f));
	}

	private static float safeInverse(float value)
	{
		return Math.abs(value) < 1.0e-6f ? 0.0f : 1.0f / value;
	}
}
