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
import com.arcanc.pulselib.content.player.animation.*;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * VIEW-space presentation of one canonical {@link PPlayerAnimationFrame}.
 *
 * <p>The camera anchor supplies the one MODEL-to-VIEW root for the entire rig.
 * Every bone and locator then uses the same resolved MODEL-space matrix that
 * third person consumes.  The vanilla arm/item adapters are rendering-boundary
 * geometry adapters; they never inspect animation translation or rotation
 * channels.</p>
 */
public final class PFirstPersonPresentation
{
	private final PPlayerAnimationFrame frame;
	private final Matrix4f modelToView;
	private final Matrix4f scratchBind = new Matrix4f();
	private final Matrix4f scratchBasis = new Matrix4f();

	/**
	 * Creates an instance of the enclosing type.
	 * @param frame the frame to use.
	 * @param modelToView the model to view to use.
	 */
	private PFirstPersonPresentation(PPlayerAnimationFrame frame,
	                                 Matrix4f modelToView)
	{
		this.frame = frame;
		this.modelToView = modelToView;
	}

	/**
	 * Performs the create operation.
	 * @param frame the frame to use.
	 * @return the value produced by this operation.
	 */
	public static @Nullable PFirstPersonPresentation create(PPlayerAnimationFrame frame)
	{
		String cameraBone = frame.definition().anchors().get(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA);
		if (cameraBone == null)
			return null;

		Matrix4f currentCamera = new Matrix4f();
		if (!frame.modelMatrix(cameraBone, currentCamera))
			return null;

		return new PFirstPersonPresentation(frame, currentCamera.invert());
	}

	/** Writes {@code M_viewRoot * M_boneModel}. */
	public boolean boneMatrix(String boneName, Matrix4f destination)
	{
		if (!this.frame.modelMatrix(boneName, destination))
			return false;
		this.modelToView.mul(destination, destination);
		return true;
	}

	/**
	 * Performs the anchor matrix operation.
	 * @param anchor the anchor to use.
	 * @param destination the destination to use.
	 * @return the value produced by this operation.
	 */
	public boolean anchorMatrix(PPlayerAnimationAnchor anchor, Matrix4f destination)
	{
		String bone = this.frame.definition().anchors().get(anchor);
		return bone != null && boneMatrix(bone, destination);
	}

	/**
	 * Matrix immediately before Minecraft applies the corresponding arm
	 * ModelPart, composed from the canonical MODEL-space arm matrix.
	 */
	public boolean armPreModelPartMatrix(PPlayerPart part, Matrix4f destination)
	{
		String bone = this.frame.definition().bindings().get(part);
		if (bone == null || !boneMatrix(bone, destination))
			return false;

		HumanoidArm arm = part == PPlayerPart.RIGHT_ARM ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
		/*
		 *  FIRST_PERSON_CAMERA is the authored first-person frame. Do not retarget
		 *  the arm animation onto Minecraft's vanilla first-person rest transform:
		 *  that pose already points the arm strongly forward and destroys authored
		 *  motions such as a down-to-forward arm raise.
		 *
		 *  boneMatrix() is the animated arm pivot in source-model VIEW space.
		 *  AvatarRenderer then applies Minecraft's arm ModelPart pivot, so use the
		 *  vanilla ModelPart only as geometry and rebase that geometry into the
		 *  source model's first-person basis:
		 *
		 * M_pre = M_cameraRelativeArm
		 *            * M_playerModelToFirstPerson
		 *            * inverse(M_modelPartPivot)
		 */
		PPlayerAnimationSpace.toPlayerGeometrySpace(
				PTransform.IDENTITY, this.frame.definition()).matrix(this.scratchBasis);
		PFirstPersonRestPose.armModelPartBind(arm).matrix(this.scratchBind).invert();

		destination.mul(this.scratchBasis).mul(this.scratchBind);
		return true;
	}

	/**
	 * Matrix immediately before Minecraft submits a held item.  Item anchors
	 * inherit their canonical skeleton hierarchy in the same way as arms.
	 */
	public boolean itemMatrix(PPlayerAnimationAnchor anchor, Matrix4f destination)
	{
		String bone = this.frame.definition().anchors().get(anchor);
		if (bone == null || !boneMatrix(bone, destination))
			return false;

		PPlayerAnimationSpace.toPlayerGeometrySpace(
				PTransform.IDENTITY,
				this.frame.definition()).
				matrix(this.scratchBasis);

		destination.mul(this.scratchBasis);
		return true;
	}
}
