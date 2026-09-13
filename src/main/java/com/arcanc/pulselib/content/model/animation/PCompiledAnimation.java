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

	/**
	 * Creates an instance of the enclosing type.
	 * @param animation the animation to use.
	 * @param boneAnimations the bone animations to use.
	 * @param boneMask the bone mask to use.
	 */
	public PCompiledAnimation(PAnimation animation, PBoneAnimation[] boneAnimations, BitSet boneMask)
	{
		this.animation = animation;
		this.boneAnimations = boneAnimations;
		this.boneMask = (BitSet) boneMask.clone();
	}

	/**
	 * Performs the animation operation.
	 * @return the value produced by this operation.
	 */
	public PAnimation animation()
	{
		return this.animation;
	}

	/**
	 * Performs the bone animation operation.
	 * @param boneIndex the bone index to use.
	 * @return the value produced by this operation.
	 */
	public PBoneAnimation boneAnimation(int boneIndex)
	{
		return this.boneAnimations[boneIndex];
	}

	/**
	 * Performs the bone mask operation.
	 * @return the value produced by this operation.
	 */
	public BitSet boneMask()
	{
		return (BitSet) this.boneMask.clone();
	}
}
