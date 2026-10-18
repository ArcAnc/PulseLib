/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
/**
 * Immutable per-renderArmWithItem state. Matrices are copied at capture time.
 *
 * @param hand the interaction hand being rendered
 * @param arm the humanoid arm whose geometry is being processed
 * @param basePose the base pose
 * @param baseNormal the base normal
 * @param presentation the presentation
 * @param map the map
 */
public record PFirstPersonRenderContext(
        InteractionHand hand,
        HumanoidArm arm,
        Matrix4f basePose,
        Matrix3f baseNormal,
	        PFirstPersonRenderPresentation presentation,
        boolean map)
{
	/**
	 * Copies the hand-pass basis and associates it with the sampled presentation.
	 *
	 * @param hand the interaction hand being rendered
	 * @param arm the humanoid arm whose geometry is being processed
	 * @param basePose the base pose
	 * @param baseNormal the base normal
	 * @param presentation the presentation
	 * @param map the map
	 */
	public PFirstPersonRenderContext
	{
		basePose = new Matrix4f(basePose);
		baseNormal = new Matrix3f(baseNormal);
	}
}
