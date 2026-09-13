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
	        PFirstPersonRenderPresentation presentation,
        boolean map)
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param hand the hand to use.
	 * @param arm the arm to use.
	 * @param basePose the base pose to use.
	 * @param baseNormal the base normal to use.
	 * @param presentation the presentation to use.
	 * @param map the map to use.
	 */
	public PFirstPersonRenderContext
	{
		basePose = new Matrix4f(basePose);
		baseNormal = new Matrix3f(baseNormal);
	}
}
