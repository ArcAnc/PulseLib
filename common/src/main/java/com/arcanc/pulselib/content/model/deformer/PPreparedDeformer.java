/**
 * @author ArcAnc
 * Created at: 08.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.deformer;

import org.joml.Vector3f;

/**
 * Executable mesh deformation operation with parameters resolved at sample time.
 */
public interface PPreparedDeformer
{
	/**
	 * Changes the supplied local position in place using current channel values.
	 *
	 * @param position the position
	 * @param values the source of current animated deformation channel values
	 */
	void deform(Vector3f position, PDeformerValueSource values);
}
