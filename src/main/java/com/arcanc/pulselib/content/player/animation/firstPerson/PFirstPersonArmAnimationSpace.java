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

	public static PTransform convert(HumanoidArm arm, PTransform sourceLocalDelta)
	{
		PTransform armOrigin = PFirstPersonRestPose.armOrigin(arm);
		Vector3f armToHand = PFirstPersonRestPose.armRig(arm).armToHand().translation();
		Vector3f handDirection = armOrigin.rotation().transform(armToHand).normalize();
		Vector3f forwardTangent = projectOntoPlane(CAMERA_FORWARD, handDirection);
		if (forwardTangent.lengthSquared() < 1.0e-10f)
			return sourceLocalDelta;
		forwardTangent.normalize();

		/* axis × handDirection = forwardTangent */
		Vector3f cameraAxis = handDirection.cross(forwardTangent).normalize();
		Vector3f localAxis = armOrigin.rotation().invert().transform(cameraAxis).normalize();
		Quaternionf sourceToVanilla = new Quaternionf().rotationTo(new Vector3f(1.0f, 0.0f, 0.0f), localAxis);
		return new PFirstPersonBasis(new Matrix4f().rotate(sourceToVanilla)).convert(sourceLocalDelta);
	}

	private static Vector3f projectOntoPlane(Vector3f vector, Vector3f normal)
	{
		return new Vector3f(vector).fma(-vector.dot(normal), normal);
	}
}
