/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import java.util.BitSet;

public final class PCompiledAnimation
{
	private final PAnimation animation;
	private final PBoneAnimation[] boneAnimations;
	private final BitSet boneMask;

	public PCompiledAnimation(PAnimation animation, PBoneAnimation[] boneAnimations, BitSet boneMask)
	{
		this.animation = animation;
		this.boneAnimations = boneAnimations;
		this.boneMask = (BitSet) boneMask.clone();
	}

	public PAnimation animation()
	{
		return this.animation;
	}

	public PBoneAnimation boneAnimation(int boneIndex)
	{
		return this.boneAnimations[boneIndex];
	}

	public BitSet boneMask()
	{
		return (BitSet) this.boneMask.clone();
	}
}
