/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;

public final class PPlayerFirstPersonArmTransform
{
	private static final Matrix4f RIGHT_ARM_CORRECTION = new Matrix4f();
	private static final Matrix4f LEFT_ARM_CORRECTION = new Matrix4f();

	private PPlayerFirstPersonArmTransform()
	{
	}

	public static Matrix4f correction(HumanoidArm arm)
	{
		return new Matrix4f(arm == HumanoidArm.RIGHT ? RIGHT_ARM_CORRECTION : LEFT_ARM_CORRECTION);
	}
}
