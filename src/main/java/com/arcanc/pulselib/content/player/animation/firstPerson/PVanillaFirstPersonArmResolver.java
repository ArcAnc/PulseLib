/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import net.minecraft.world.entity.HumanoidArm;
/** Resolves the origin expected by Minecraft's one-segment arm renderer. */
public final class PVanillaFirstPersonArmResolver
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PVanillaFirstPersonArmResolver()
	{
	}

	/**
	 * Resolves the arm origin.
	 * @param arm the arm to use.
	 * @param handTarget the hand target to use.
	 * @return the value produced by this operation.
	 */
	public static PTransform resolveArmOrigin(HumanoidArm arm, PTransform handTarget)
	{
		PFirstPersonArmRig rig = PFirstPersonRestPose.armRig(arm);
		return handTarget.compose(rig.armToHand().inverse());
	}
}
