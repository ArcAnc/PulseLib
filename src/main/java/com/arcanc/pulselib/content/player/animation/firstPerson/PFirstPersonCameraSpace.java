/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Boundary conversion from Pulse first-person space to Minecraft camera space. */
public final class PFirstPersonCameraSpace
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PFirstPersonCameraSpace()
	{
	}

	/**
	 * Performs the to minecraft offset operation.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	public static Vector3f toMinecraftOffset(Vector3f value)
	{
		return value.set(-value.x, -value.y, value.z);
	}

	/**
	 * Performs the to minecraft rotation operation.
	 * @param value the value to use.
	 * @return the value produced by this operation.
	 */
	public static Quaternionf toMinecraftRotation(Quaternionf value)
	{
		return value.set(-value.x, -value.y, value.z, value.w);
	}
}
