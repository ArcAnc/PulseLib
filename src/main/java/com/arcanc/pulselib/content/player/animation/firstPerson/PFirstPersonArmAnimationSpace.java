package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Converts an authored arm-local delta into the local coordinates of
 * Minecraft's one-segment first-person arm.
 */
public final class PFirstPersonArmAnimationSpace
{
	private static final Vector3f CAMERA_FORWARD = new Vector3f(0.0f, 0.0f, -1.0f);

	private PFirstPersonArmAnimationSpace()
	{
	}
	
	public static PTransform convert(
			PTransform sourceBind,
			PTransform vanillaBind,
			PTransform sourceLocalDelta)
	{
		Quaternionf sourceToVanilla =
				new Quaternionf(vanillaBind.rotation()).
						invert().
						mul(sourceBind.rotation()).
						normalize();
		
		return new PFirstPersonBasis(
				new Matrix4f().rotate(sourceToVanilla)
		).convert(sourceLocalDelta);
	}

	private static Vector3f projectOntoPlane(Vector3f vector, Vector3f normal)
	{
		return new Vector3f(vector).fma(-vector.dot(normal), normal);
	}
}
