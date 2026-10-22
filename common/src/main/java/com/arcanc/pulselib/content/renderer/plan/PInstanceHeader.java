/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer.plan;

import org.joml.Matrix4f;

/**
 * Per-instance model transform, packed render attributes and optional GPU deformation stream offsets.
 *
 * @param transform the transform
 * @param packedColor the packed ARGB vertex color
 * @param packedLight the packed block and sky light coordinates
 * @param packedOverlay the packed overlay texture coordinates
 * @param auxiliaryOperationOffset the GPU deformer operation-stream offset
 * @param auxiliaryValueOffset the GPU deformer value-stream offset
 * @param auxiliaryOperationCount the number of GPU deformer operations applied to this instance
 */
public record PInstanceHeader(
		Matrix4f transform,
		int packedColor,
		int packedLight,
		int packedOverlay,
		int auxiliaryOperationOffset,
		int auxiliaryValueOffset,
		int auxiliaryOperationCount)
{
	/**
	 * Copies the instance transform and stores packed render attributes and optional deformation offsets.
	 *
	 * @param transform the transform
	 * @param packedColor the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param auxiliaryOperationOffset the GPU deformer operation-stream offset
	 * @param auxiliaryValueOffset the GPU deformer value-stream offset
	 * @param auxiliaryOperationCount the number of GPU deformer operations applied to this instance
	 */
	public PInstanceHeader
	{
		transform = new Matrix4f(transform);
	}

	/**
	 * Copies the instance transform and stores packed render attributes and optional deformation offsets.
	 *
	 * @param transform the transform
	 * @param packedColor the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 */
	public PInstanceHeader(Matrix4f transform, int packedColor, int packedLight, int packedOverlay)
	{
		this(transform, packedColor, packedLight, packedOverlay, -1, -1, 0);
	}
}
