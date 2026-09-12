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
 * Canonical camera-space sockets at animation delta identity.
 */
public record PFirstPersonRestPose(
        PTransform rightHand,
        PTransform leftHand,
        PTransform rightItem,
        PTransform leftItem)
{
	public static final PFirstPersonRestPose VANILLA = new PFirstPersonRestPose(
				handTransform(HumanoidArm.RIGHT), handTransform(HumanoidArm.LEFT),
				itemTransform(HumanoidArm.RIGHT), itemTransform(HumanoidArm.LEFT));

	public static PTransform hand(HumanoidArm arm)
	{
		return arm == HumanoidArm.RIGHT ? VANILLA.rightHand : VANILLA.leftHand;
	}

	public static PTransform item(HumanoidArm arm)
	{
		return arm == HumanoidArm.RIGHT ? VANILLA.rightItem : VANILLA.leftItem;
	}

	/** Kept for source compatibility; arms are authored through the hand socket. */
	public static PTransform arm(HumanoidArm arm)
	{
		return hand(arm);
	}

	/** Pose expected by AvatarRenderer immediately before it applies PlayerModel's arm part. */
	public static PTransform armOrigin(HumanoidArm arm)
	{
		return armTransform(arm);
	}

	public static PFirstPersonArmRig armRig(HumanoidArm arm)
	{
		/*
		 * AvatarRenderer receives the pose before PlayerModel's arm ModelPart is
		 * applied. The palm therefore sits at the arm pivot plus its 12-pixel
		 * cube length: (-/+6, 12, 0) / 16 for regular player arms.
		 */
		float side = arm == HumanoidArm.RIGHT ? -1.0f : 1.0f;
		return new PFirstPersonArmRig(PTransform.IDENTITY,
				PTransform.translation(new Vector3f(side * 0.375f, 0.75f, 0.0f)));
	}

	public static PFirstPersonItemRig itemRig(HumanoidArm arm)
	{
		return new PFirstPersonItemRig(PTransform.IDENTITY, PTransform.IDENTITY);
	}

	private static PTransform armTransform(HumanoidArm arm)
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

	private static PTransform handTransform(HumanoidArm arm)
	{
		PFirstPersonArmRig rig = armRig(arm);
		return armTransform(arm).compose(rig.armToHand());
	}

	private static PTransform itemTransform(HumanoidArm arm)
	{
		float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
		return PTransform.translation(new Vector3f(side * 0.56f, -0.52f, -0.72f));
	}
}
