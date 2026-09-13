package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.player.animation.PBoneRenderMask;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.PPlayerPresentation;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * VIEW-space presentation of one canonical {@link PPlayerAnimationFrame}.
 *
 * <p>The camera anchor supplies the one MODEL-to-VIEW root for the entire rig.
 * Every bone and locator then uses the same resolved MODEL-space matrix that
 * third person consumes.  The vanilla arm/item adapters are rendering-boundary
 * geometry adapters; they never inspect animation translation or rotation
 * channels.</p>
 */
public final class PFirstPersonPresentation implements PPlayerPresentation
{
	private final PPlayerAnimationFrame frame;
	private final Matrix4f modelToView;
	private final Matrix4f bindModelToView;
	private final PBoneRenderMask renderMask;
	private final Matrix4f scratchBind = new Matrix4f();
	private final Matrix4f scratchRest = new Matrix4f();

	private PFirstPersonPresentation(PPlayerAnimationFrame frame,
	                                 Matrix4f modelToView,
	                                 Matrix4f bindModelToView,
	                                 PBoneRenderMask renderMask)
	{
		this.frame = frame;
		this.modelToView = modelToView;
		this.bindModelToView = bindModelToView;
		this.renderMask = renderMask;
	}

	public static @Nullable PFirstPersonPresentation create(PPlayerAnimationFrame frame)
	{
		String cameraBone = frame.definition().anchors().get(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA);
		if (cameraBone == null)
			return null;

		Matrix4f currentCamera = new Matrix4f();
		Matrix4f bindCamera = new Matrix4f();
		if (!frame.modelMatrix(cameraBone, currentCamera) || !frame.bindModelMatrix(cameraBone, bindCamera))
			return null;

		return new PFirstPersonPresentation(frame, currentCamera.invert(), bindCamera.invert(),
				frame.definition().firstPersonRenderMask());
	}

	@Override
	public PBoneRenderMask renderMask()
	{
		return this.renderMask;
	}

	public PFirstPersonSkeletonMode skeletonMode()
	{
		return PFirstPersonSkeletonMode.SAME_SKELETON;
	}

	/** Writes {@code M_viewRoot * M_boneModel}. */
	@Override
	public boolean boneMatrix(PPlayerAnimationFrame frame, String boneName, Matrix4f destination)
	{
		if (frame != this.frame)
			throw new IllegalArgumentException("A player presentation can only consume its resolved canonical frame");
		if (!this.frame.modelMatrix(boneName, destination))
			return false;
		this.modelToView.mul(destination, destination);
		return true;
	}

	public boolean boneMatrix(String boneName, Matrix4f destination)
	{
		return boneMatrix(this.frame, boneName, destination);
	}

	/** Writes the shared MODEL-to-VIEW presentation root for this entire rig. */
	public Matrix4f presentationRoot(Matrix4f destination)
	{
		return destination.set(this.modelToView);
	}

	public boolean anchorMatrix(PPlayerAnimationAnchor anchor, Matrix4f destination)
	{
		String bone = this.frame.definition().anchors().get(anchor);
		return bone != null && boneMatrix(bone, destination);
	}

	/**
	 * Matrix immediately before Minecraft applies the corresponding arm
	 * ModelPart.  The delta is formed only as matrix composition of canonical
	 * MODEL-space current/bind transforms.
	 */
	public boolean armPreModelPartMatrix(PPlayerPart part, Matrix4f destination)
	{
		String bone = this.frame.definition().bindings().get(part);
		if (bone == null || !boneMatrix(bone, destination) || !this.frame.bindModelMatrix(bone, this.scratchBind))
			return false;

		this.bindModelToView.mul(this.scratchBind, this.scratchBind).invert();
		destination.mul(this.scratchBind);
		HumanoidArm arm = part == PPlayerPart.RIGHT_ARM ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
		PFirstPersonRestPose.armOrigin(arm).matrix(this.scratchRest);
		destination.mul(this.scratchRest);
		return true;
	}

	/**
	 * Matrix immediately before Minecraft submits a held item.  Item anchors
	 * inherit their canonical skeleton hierarchy in the same way as arms.
	 */
	public boolean itemMatrix(PPlayerAnimationAnchor anchor, Matrix4f destination)
	{
		String bone = this.frame.definition().anchors().get(anchor);
		if (bone == null || !boneMatrix(bone, destination) || !this.frame.bindModelMatrix(bone, this.scratchBind))
			return false;

		this.bindModelToView.mul(this.scratchBind, this.scratchBind).invert();
		destination.mul(this.scratchBind);
		HumanoidArm arm = anchor.equals(PPlayerAnimationAnchors.RIGHT_ITEM) ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
		PFirstPersonRestPose.item(arm).matrix(this.scratchRest);
		destination.mul(this.scratchRest);
		return true;
	}
}
