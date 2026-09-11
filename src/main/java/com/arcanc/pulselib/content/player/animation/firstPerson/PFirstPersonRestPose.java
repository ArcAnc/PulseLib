/**
 * @author ArcAnc
 * Created at: 11.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Canonical first-person rest transforms. They match Minecraft's steady hand
 * grips, allowing the PulseLib renderer to own both ends of an animation fade.
 */
public final class PFirstPersonRestPose
{
	private PFirstPersonRestPose()
	{
	}

	public static PTransform arm(HumanoidArm arm)
	{
		float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
		return PTransform.IDENTITY.
				compose(PTransform.translation(new Vector3f(side * 0.64000005f, -0.6f, -0.71999997f))).
				compose(PTransform.rotation(new Quaternionf().rotationY((float)Math.toRadians(side * 45.0f)))).
				compose(PTransform.translation(new Vector3f(side * -1.0f, 3.6f, 3.5f))).
				compose(PTransform.rotation(new Quaternionf().rotationZ((float)Math.toRadians(side * 120.0f)))).
				compose(PTransform.rotation(new Quaternionf().rotationX((float)Math.toRadians(200.0f)))).
				compose(PTransform.rotation(new Quaternionf().rotationY((float)Math.toRadians(side * -135.0f)))).
				compose(PTransform.translation(new Vector3f(side * 5.6f, 0.0f, 0.0f)));
	}

	public static PTransform item(HumanoidArm arm)
	{
		float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
		return PTransform.translation(new Vector3f(side * 0.56f, -0.52f, -0.72f));
	}
}
