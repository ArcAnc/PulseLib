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
	 * Creates a vanilla first person arm resolver.
	 */
	private PVanillaFirstPersonArmResolver()
	{
	}

	/**
	 * Converts the animated hand socket target into the vanilla arm render-origin transform.
	 *
	 * @param arm the humanoid arm whose geometry is being processed
	 * @param handTarget the hand target
	 * @return the vanilla arm render-origin transform matching the target hand socket
	 */
	public static PTransform resolveArmOrigin(HumanoidArm arm, PTransform handTarget)
	{
		PFirstPersonArmRig rig = PFirstPersonRestPose.armRig(arm);
		return handTarget.compose(rig.armToHand().inverse());
	}
}
