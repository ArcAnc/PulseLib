/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import org.joml.Vector3f;

/**
 * Converts sampled vectors from a source format into PulseLib animation coordinates.
 */
@FunctionalInterface
public interface PVectorConversion
{
	/**
	 * Leaves sampled vectors unchanged.
	 */
	PVectorConversion IDENTITY = value -> {};

	/**
	 * Converts the supplied vector in place.
	 *
	 * @param value the sampled animation channel value
	 */
	void apply(Vector3f value);
}
