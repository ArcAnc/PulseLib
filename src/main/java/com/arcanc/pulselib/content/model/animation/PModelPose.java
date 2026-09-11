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

public final class PModelPose
{
	private final PTransform[] transforms;
	private final BitSet validBones = new BitSet();

	public PModelPose(int boneCount)
	{
		this.transforms = new PTransform[boneCount];
		for (int index = 0; index < boneCount; index++)
			this.transforms[index] = PTransform.IDENTITY;
	}

	public PTransform transform(int boneIndex)
	{
		return this.transforms[boneIndex];
	}
	
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

	private void updateBone(PBakedModel model, PPose pose, int index)
	{
		int parent = model.parentIndex(index);
		PTransform local = new PTransform(
				pose.translation(index), pose.rotation(index), pose.scale(index));
		this.transforms[index] = parent < 0 ? local : this.transforms[parent].compose(local);
		this.validBones.set(index);
	}
}
