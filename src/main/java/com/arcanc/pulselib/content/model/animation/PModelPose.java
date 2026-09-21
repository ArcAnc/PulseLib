/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import com.arcanc.pulselib.content.model.baked.PBakedModel;
import java.util.BitSet;

/**
 * Resolved MODEL-space transforms.  {@link PPose} supplies LOCAL_BONE TRS;
 * this cache applies the baked parent hierarchy once for consumers such as
 * render presentations, locators and deformers.
 */
public final class PModelPose
{
	private final PTransform[] transforms;
	private final BitSet validBones = new BitSet();

	/**
	 * Creates an instance of the enclosing type.
	 * @param boneCount the bone count to use.
	 */
	public PModelPose(int boneCount)
	{
		this.transforms = new PTransform[boneCount];
		for (int index = 0; index < boneCount; index++)
			this.transforms[index] = PTransform.IDENTITY;
	}

	/** Returns a resolved MODEL-space bone transform. */
	public PTransform transform(int boneIndex)
	{
		return this.transforms[boneIndex];
	}

	/**
	 * Performs the update operation.
	 * @param model the model to use.
	 * @param localPose the local pose to use.
	 */
	public void update(PBakedModel model, PPose localPose)
	{
		BitSet update = localPose.dirtyBones();
		for (int index = 0; index < this.transforms.length; index++)
		{
			int parent = model.parentIndex(index);
			if (!this.validBones.get(index) || (parent >= 0 && update.get(parent)))
				update.set(index);
		}
		for (int index = update.nextSetBit(0); index >= 0; index = update.nextSetBit(index + 1))
			updateBone(model, localPose, index);
	}

	/**
	 * Updates the bone.
	 * @param model the model to use.
	 * @param pose the pose to use.
	 * @param index the index to use.
	 */
	private void updateBone(PBakedModel model, PPose pose, int index)
	{
		int parent = model.parentIndex(index);
		PTransform local = new PTransform(
				pose.translation(index), pose.rotation(index), pose.scale(index));
		this.transforms[index] = parent < 0 ? local : this.transforms[parent].compose(local);
		this.validBones.set(index);
	}
}
