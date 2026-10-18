/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Receives sampled translation, rotation and scale values for indexed bones.
 */
public interface PPoseWriter
{
	/**
	 * Writes the translation channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param value the new local translation value
	 */
	void translation(int boneIndex, Vector3f value);

	/**
	 * Writes the rotation channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param value the new local rotation value
	 */
	void rotation(int boneIndex, Quaternionf value);

	/**
	 * Writes the scale channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param value the new local scale value
	 */
	void scale(int boneIndex, Vector3f value);
}
