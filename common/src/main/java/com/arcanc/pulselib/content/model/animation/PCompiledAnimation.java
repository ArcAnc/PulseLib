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

/**
 * Animation clip indexed by baked-model bone indices with a mask of participating bones.
 */
public final class PCompiledAnimation
{
	private final PAnimation animation;
	private final PBoneAnimation[] boneAnimations;
	private final BitSet boneMask;

	/**
	 * Associates an animation with tracks indexed for one baked-model hierarchy.
	 *
	 * @param animation the animation clip to sample
	 * @param boneAnimations the animation tracks associated with model bones
	 * @param boneMask the mask of bones participating in the animation
	 */
	public PCompiledAnimation(PAnimation animation, PBoneAnimation[] boneAnimations, BitSet boneMask)
	{
		this.animation = animation;
		this.boneAnimations = boneAnimations;
		this.boneMask = (BitSet) boneMask.clone();
	}

	/**
	 * Returns the animation.
	 *
	 * @return the animation
	 */
	public PAnimation animation()
	{
		return this.animation;
	}

	/**
	 * Returns the animation tracks for a baked bone index, or {@code null} when that bone has no tracks.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @return the animation tracks for a baked bone index, or {@code null} when that bone has no tracks
	 */
	public PBoneAnimation boneAnimation(int boneIndex)
	{
		return this.boneAnimations[boneIndex];
	}

	/**
	 * Returns the mask of baked bones that have tracks in this compiled animation.
	 *
	 * @return the mask of baked bones that have tracks in this compiled animation
	 */
	public BitSet boneMask()
	{
		return (BitSet) this.boneMask.clone();
	}
}
