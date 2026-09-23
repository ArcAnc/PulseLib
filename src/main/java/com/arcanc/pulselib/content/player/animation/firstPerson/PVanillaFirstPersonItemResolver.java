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
/** Resolves the origin expected by ItemInHandRenderer's final item draw call. */
public final class PVanillaFirstPersonItemResolver
{
	/**
	 * Creates an instance of the enclosing type.
	 */
	private PVanillaFirstPersonItemResolver()
	{
	}

	/**
	 * Resolves the item origin.
	 * @param arm the arm to use.
	 * @param itemTarget the item target to use.
	 * @return the value produced by this operation.
	 */
	public static PTransform resolveItemOrigin(HumanoidArm arm, PTransform itemTarget)
	{
		PFirstPersonItemRig rig = PFirstPersonRestPose.itemRig(arm);
		return itemTarget.compose(rig.originToSocket().inverse());
	}
}
