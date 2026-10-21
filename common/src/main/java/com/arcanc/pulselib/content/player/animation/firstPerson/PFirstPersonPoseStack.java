package com.arcanc.pulselib.content.player.animation.firstPerson;
import com.arcanc.pulselib.content.model.animation.PTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
/** Replaces an overridden local pose without deriving anything from vanilla's transient pose. */
public final class PFirstPersonPoseStack
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PFirstPersonPoseStack()
	{
	}

	/**
	 * Replaces the local pose.
	 * @param poseStack the pose stack to use.
	 * @param basePose the base pose to use.
	 * @param baseNormal the base normal to use.
	 * @param localTransform the local transform to use.
	 */
	public static void replaceLocalPose(PoseStack poseStack,
	                                    Matrix4f basePose,
	                                    Matrix3f baseNormal,
	                                    PTransform localTransform)
	{
		Matrix4f local = localTransform.matrix();
		poseStack.last().pose().set(basePose).mul(local);

		Matrix3f localNormal = new Matrix3f(local);
		if (Math.abs(localNormal.determinant()) > 1.0e-6f)
			localNormal.invert().transpose();
		else
			localNormal.identity();
		poseStack.last().normal().set(baseNormal).mul(localNormal);
	}
}
