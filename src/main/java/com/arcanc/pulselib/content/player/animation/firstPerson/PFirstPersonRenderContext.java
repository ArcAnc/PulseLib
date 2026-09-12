package com.arcanc.pulselib.content.player.animation.firstPerson;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Immutable per-renderArmWithItem state. Matrices are copied at capture time. */
public record PFirstPersonRenderContext(
        InteractionHand hand,
        HumanoidArm arm,
        Matrix4f basePose,
        Matrix3f baseNormal,
        PPlayerFirstPersonPose animationPose,
        boolean map)
{
	public PFirstPersonRenderContext
	{
		basePose = new Matrix4f(basePose);
		baseNormal = new Matrix3f(baseNormal);
	}
}
