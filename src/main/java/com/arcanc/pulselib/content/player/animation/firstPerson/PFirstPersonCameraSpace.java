package com.arcanc.pulselib.content.player.animation.firstPerson;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Boundary conversion from Pulse first-person space to Minecraft camera space. */
public final class PFirstPersonCameraSpace
{
	private PFirstPersonCameraSpace()
	{
	}

	public static Vector3f toMinecraftOffset(Vector3f value)
	{
		return value.set(-value.x, -value.y, value.z);
	}

	public static Quaternionf toMinecraftRotation(Quaternionf value)
	{
		return value.set(-value.x, -value.y, value.z, value.w);
	}
}
