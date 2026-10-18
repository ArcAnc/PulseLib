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
 * Local offset, rotation and scale applied when attaching a model bone to an entity model part.
 *
 * @param offset the local translation offset
 * @param rotation the local quaternion rotation
 * @param scale the local scale vector
 */
public record PTransform(Vector3f offset, Quaternionf rotation, Vector3f scale)
{
	/**
	 * Attachment transform with no offset or rotation and unit scale.
	 */
	public static final PTransform IDENTITY = new PTransform(
			new Vector3f(),
			new Quaternionf(),
			new Vector3f(1, 1, 1));
	
	/**
	 * Copies attachment offset, rotation and scale into independent storage.
	 *
	 * @param offset the local translation offset
	 * @param rotation the local quaternion rotation
	 * @param scale the local scale vector
	 */
	public PTransform
	{
		offset = new Vector3f(Objects.requireNonNull(offset));
		rotation = new Quaternionf(Objects.requireNonNull(rotation));
		scale = new Vector3f(Objects.requireNonNull(scale));
	}
	
	/**
	 * Returns a defensive copy of the offset.
	 *
	 * @return a defensive copy of the offset
	 */
	@Override
	public Vector3f offset()
	{
		return new Vector3f(this.offset);
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
	 * Creates an attachment transform from an offset, Euler rotation and scale.
	 *
	 * @param offset the local translation offset
	 * @param rotation the local quaternion rotation
	 * @param scale the local scale vector
	 * @return the local attachment transform built from the supplied vectors
	 */
	public static PTransform of(Vector3f offset, Vector3f rotation, Vector3f scale)
	{
		return new PTransform(offset, eulerDegreesToQuaternion(rotation), scale);
	}
	
	private static Quaternionf eulerDegreesToQuaternion(Vector3f rotation)
	{
		Objects.requireNonNull(rotation);
		return new Quaternionf().rotationXYZ(
				(float)Math.toRadians(rotation.x()),
				(float)Math.toRadians(rotation.y()),
				(float)Math.toRadians(rotation.z()));
	}
}
