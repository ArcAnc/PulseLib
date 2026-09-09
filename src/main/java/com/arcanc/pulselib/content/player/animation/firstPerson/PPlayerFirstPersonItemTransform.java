/**
 * @author ArcAnc
 * Created at: 09.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public final class PPlayerFirstPersonItemTransform
{
	private static final Matrix4f MAIN_HAND_GRIP_CORRECTION = new Matrix4f();
	private static final Matrix4f OFF_HAND_GRIP_CORRECTION = new Matrix4f();

	private PPlayerFirstPersonItemTransform()
	{
	}

	public static Matrix4f correction(ItemStack stack, InteractionHand hand)
	{
		return new Matrix4f(hand == InteractionHand.MAIN_HAND ? MAIN_HAND_GRIP_CORRECTION : OFF_HAND_GRIP_CORRECTION);
	}
}
