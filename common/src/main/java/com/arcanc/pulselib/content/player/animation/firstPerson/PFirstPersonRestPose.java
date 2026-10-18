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
 *
 * @param rightHand the right hand
 * @param leftHand the left hand
 * @param rightItem the right item
 * @param leftItem the left item
 */
public record PFirstPersonRestPose(
        PTransform rightHand,
        PTransform leftHand,
        PTransform rightItem,
        PTransform leftItem)
{
	/**
	 * Default reference pose matching vanilla first-person arm and item rendering.
	 */
	public static final PFirstPersonRestPose VANILLA = new PFirstPersonRestPose(
				handTransform(HumanoidArm.RIGHT), handTransform(HumanoidArm.LEFT),
				itemTransform(HumanoidArm.RIGHT), itemTransform(HumanoidArm.LEFT));

	/**
	 * Returns the vanilla held-item rest transform for the selected arm.
	 *
	 * @param arm the humanoid arm whose geometry is being processed
	 * @return the vanilla held-item rest transform for the selected arm
	 */
	public static PTransform item(HumanoidArm arm)
	{
		return arm == HumanoidArm.RIGHT ? VANILLA.rightItem : VANILLA.leftItem;
	}

	/**
	 * Returns the vanilla arm-origin and hand-socket reference rig for the selected arm.
	 *
	 * @param arm the humanoid arm whose geometry is being processed
	 * @return the vanilla arm-origin and hand-socket reference rig for the selected arm
	 */
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

	/**
	 * Returns the vanilla item-origin and attachment-socket reference rig for the selected arm.
	 *
	 * @param arm the humanoid arm whose geometry is being processed
	 * @return the vanilla item-origin and attachment-socket reference rig for the selected arm
	 */
	public static PFirstPersonItemRig itemRig(HumanoidArm arm)
	{
		return new PFirstPersonItemRig(PTransform.IDENTITY, PTransform.IDENTITY);
	}

	/**
	 * Builds the canonical rest transform of the selected vanilla arm origin.
	 *
	 * @param arm the humanoid arm whose geometry is being processed
	 * @return the constructed canonical rest transform of the selected vanilla arm origin
	 */
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

	/**
	 * Builds the canonical hand socket transform of the selected arm.
	 *
	 * @param arm the humanoid arm whose geometry is being processed
	 * @return the constructed canonical hand socket transform of the selected arm
	 */
	private static PTransform handTransform(HumanoidArm arm)
	{
		PFirstPersonArmRig rig = armRig(arm);
		return armTransform(arm).compose(rig.armToHand());
	}

	/**
	 * Builds the canonical rest transform of the selected held-item socket.
	 *
	 * @param arm the humanoid arm whose geometry is being processed
	 * @return the constructed canonical rest transform of the selected held-item socket
	 */
	private static PTransform itemTransform(HumanoidArm arm)
	{
		float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
		return PTransform.translation(new Vector3f(side * 0.56f, -0.52f, -0.72f));
	}

	/**
	 * Bind transform applied by the vanilla arm ModelPart after AvatarRenderer receives the PoseStack.
	 *
	 * @param arm the humanoid arm whose geometry is being processed
	 * @return the unanimated vanilla arm ModelPart transform
	 */
	public static PTransform armModelPartBind(HumanoidArm arm)
	{
		float side = arm == HumanoidArm.RIGHT ? -1.0f : 1.0f;
		return PTransform.translation(new Vector3f(side * (5.0f / 16.0f), 2.0f / 16.0f, 0.0f));
	}
}
