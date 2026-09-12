package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Converts an authored arm-local delta into the local coordinates of
 * Minecraft's one-segment first-person arm.
 */
public final class PFirstPersonArmAnimationSpace
{

	private PFirstPersonArmAnimationSpace()
	{
	}
	
	public static Quaternionf convertRotation(
			PTransform sourceBind,
			PTransform vanillaBind,
			Quaternionf sourceDelta)
	{
		Quaternionf sourceToVanilla =
				vanillaBind.rotation().
						invert().
						mul(sourceBind.rotation()).
						normalize();
		
		Quaternionf vanillaToSource =
				new Quaternionf(sourceToVanilla).
						invert();
		
		return new Quaternionf(sourceToVanilla).
				mul(sourceDelta).
				mul(vanillaToSource).
				normalize();
	}
}
