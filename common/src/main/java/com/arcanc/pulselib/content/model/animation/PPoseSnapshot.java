/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

/** A private frozen source. Neither capture nor sampling exposes its mutable storage. */
final class PPoseSnapshot
{
	private final PPose pose;

	/**
	 * Captures an independent copy of the supplied pose.
	 *
	 * @param pose the indexed bone pose
	 */
	PPoseSnapshot(PPose pose)
	{
		this.pose = pose.copy();
	}

	/**
	 * Returns an independent copy of the captured pose.
	 *
	 * @return an independent copy of the captured pose
	 */
	PPose sample()
	{
		return this.pose.copy();
	}
}
