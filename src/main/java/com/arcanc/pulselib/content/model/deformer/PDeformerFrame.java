/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Immutable value object representing deformer frame.
 */
public record PDeformerFrame(Vector3f position, Vector3f right, Vector3f up, Vector3f forward)
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param position the position to use.
	 * @param right the right to use.
	 * @param up the up to use.
	 * @param forward the forward to use.
	 */
	public PDeformerFrame
	{
		position = new Vector3f(position);
		right = new Vector3f(right);
		up = new Vector3f(up);
		forward = new Vector3f(forward);
	}

	/**
	 * Performs the matrix operation.
	 * @return the value produced by this operation.
	 */
	public Matrix4f matrix()
	{
		return new Matrix4f().identity().
						m00(this.right.x).m01(this.right.y).m02(this.right.z).
						m10(this.up.x).m11(this.up.y).m12(this.up.z).
						m20(this.forward.x).m21(this.forward.y).m22(this.forward.z).
						m30(this.position.x).m31(this.position.y).m32(this.position.z);
	}
}
