/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * Immutable value object representing transform.
 */
public record PTransform(Vector3f offset, Quaternionf rotation, Vector3f scale)
{
	public static final PTransform IDENTITY = new PTransform(
			new Vector3f(),
			new Quaternionf(),
			new Vector3f(1, 1, 1));
	
	/**
	 * Creates an instance of the enclosing type.
	 * @param offset the offset to use.
	 * @param rotation the rotation to use.
	 * @param scale the scale to use.
	 */
	public PTransform
	{
		offset = new Vector3f(Objects.requireNonNull(offset));
		rotation = new Quaternionf(Objects.requireNonNull(rotation));
		scale = new Vector3f(Objects.requireNonNull(scale));
	}
	
	/**
	 * Performs the offset operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public Vector3f offset()
	{
		return new Vector3f(this.offset);
	}
	
	/**
	 * Performs the rotation operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public Quaternionf rotation()
	{
		return new Quaternionf(this.rotation);
	}
	
	/**
	 * Performs the scale operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public Vector3f scale()
	{
		return new Vector3f(this.scale);
	}
	
	/**
	 * Performs the of operation.
	 * @param offset the offset to use.
	 * @param rotation the rotation to use.
	 * @param scale the scale to use.
	 * @return the value produced by this operation.
	 */
	public static PTransform of(Vector3f offset, Vector3f rotation, Vector3f scale)
	{
		return new PTransform(offset, eulerDegreesToQuaternion(rotation), scale);
	}
	
	/**
	 * Performs the euler degrees to quaternion operation.
	 * @param rotation the rotation to use.
	 * @return the value produced by this operation.
	 */
	private static Quaternionf eulerDegreesToQuaternion(Vector3f rotation)
	{
		Objects.requireNonNull(rotation);
		return new Quaternionf().rotationXYZ(
				(float)Math.toRadians(rotation.x()),
				(float)Math.toRadians(rotation.y()),
				(float)Math.toRadians(rotation.z()));
	}
}
