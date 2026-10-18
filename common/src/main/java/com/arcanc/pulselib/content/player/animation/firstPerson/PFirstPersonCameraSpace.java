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
	 * Creates a first person camera space.
	 */
	private PFirstPersonCameraSpace()
	{
	}

	/**
	 * Reconstructs Minecraft yaw/pitch/roll using the native NeoForge camera convention.
	 *
	 * @param yaw the yaw angle in degrees
	 * @param pitch the pitch angle in degrees
	 * @param roll the roll angle in degrees
	 * @param destination the mutable object receiving the result
	 * @return the quaternion rotation
	 */
	public static Quaternionf fromMinecraftAngles(float yaw, float pitch, float roll, Quaternionf destination)
	{
		return destination.rotationYXZ((float)Math.PI - (float)Math.toRadians(yaw),
				-(float)Math.toRadians(pitch), -(float)Math.toRadians(roll));
	}

	/**
	 * Converts an animation camera offset into Minecraft camera coordinates.
	 *
	 * @param value the value written by this operation
	 * @return the three-component vector
	 */
	public static Vector3f toMinecraftOffset(Vector3f value)
	{
		return value.set(-value.x, -value.y, value.z);
	}

	/**
	 * Converts animation camera rotation into the Minecraft camera basis.
	 *
	 * @param value the value written by this operation
	 * @return the quaternion rotation
	 */
	public static Quaternionf toMinecraftRotation(Quaternionf value)
	{
		return value.set(-value.x, -value.y, value.z, value.w);
	}
}
