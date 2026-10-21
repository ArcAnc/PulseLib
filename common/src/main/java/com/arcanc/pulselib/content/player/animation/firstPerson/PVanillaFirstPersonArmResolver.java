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
