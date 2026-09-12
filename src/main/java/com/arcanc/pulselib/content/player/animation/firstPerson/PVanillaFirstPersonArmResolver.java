package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import net.minecraft.world.entity.HumanoidArm;

/** Resolves the origin expected by Minecraft's one-segment arm renderer. */
public final class PVanillaFirstPersonArmResolver
{
	private PVanillaFirstPersonArmResolver()
	{
	}

	public static PTransform resolveArmOrigin(HumanoidArm arm, PTransform handTarget)
	{
		PFirstPersonArmRig rig = PFirstPersonRestPose.armRig(arm);
		return handTarget.compose(rig.armToHand().inverse());
	}
}
