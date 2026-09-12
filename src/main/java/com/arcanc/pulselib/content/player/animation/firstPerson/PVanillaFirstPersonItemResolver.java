package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.model.animation.PTransform;
import net.minecraft.world.entity.HumanoidArm;

/** Resolves the origin expected by ItemInHandRenderer's final item draw call. */
public final class PVanillaFirstPersonItemResolver
{
	private PVanillaFirstPersonItemResolver()
	{
	}

	public static PTransform resolveItemOrigin(HumanoidArm arm, PTransform itemTarget)
	{
		PFirstPersonItemRig rig = PFirstPersonRestPose.itemRig(arm);
		return itemTarget.compose(rig.originToSocket().inverse());
	}
}
