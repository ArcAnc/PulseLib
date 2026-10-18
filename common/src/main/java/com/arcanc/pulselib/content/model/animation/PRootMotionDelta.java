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
 * Animation-space displacement data. Extracting it never moves an entity or its hitbox.
 *
 * @param translation the local translation vector
 * @param rotation the local quaternion rotation
 */
public record PRootMotionDelta(
		Vector3f translation,
		Quaternionf rotation)
{
	/**
	 * Returns a root-motion delta with no translation or rotation.
	 *
	 * @return a root-motion delta with no translation or rotation
	 */
	public static PRootMotionDelta identity()
	{
		return new PRootMotionDelta(new Vector3f(), new Quaternionf());
	}
}
