/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.BitSet;

public final class PPose implements PPoseWriter
{
	private final Vector3f[] translations;
	private final Quaternionf[] rotations;
	private final Vector3f[] scales;
	private final float[] scalarChannels;
	private final BitSet dirtyBones;
	private final BitSet validBones;

	/**
	 * Creates an instance of the enclosing type.
	 * @param boneCount the bone count to use.
	 */
	public PPose(int boneCount)
	{
		this(boneCount, 0);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param boneCount the bone count to use.
	 * @param scalarChannelCount the scalar channel count to use.
	 */
	public PPose(int boneCount, int scalarChannelCount)
	{
		if (boneCount < 0 || scalarChannelCount < 0)
			throw new IllegalArgumentException("Pose sizes must be non-negative");
		this.translations = new Vector3f[boneCount];
		this.rotations = new Quaternionf[boneCount];
		this.scales = new Vector3f[boneCount];
		for (int index = 0; index < boneCount; index++)
		{
			this.translations[index] = new Vector3f();
			this.rotations[index] = new Quaternionf();
			this.scales[index] = new Vector3f(1f);
		}
		this.scalarChannels = new float[scalarChannelCount];
		this.dirtyBones = new BitSet(boneCount);
		this.validBones = new BitSet(boneCount);
	}

	/**
	 * Performs the bone count operation.
	 * @return the value produced by this operation.
	 */
	public int boneCount()
	{
		return this.translations.length;
	}

	/**
	 * Performs the translation operation.
	 * @param boneIndex the bone index to use.
	 * @return the value produced by this operation.
	 */
	public Vector3f translation(int boneIndex)
	{
		return this.translations[boneIndex];
	}

	/**
	 * Performs the rotation operation.
	 * @param boneIndex the bone index to use.
	 * @return the value produced by this operation.
	 */
	public Quaternionf rotation(int boneIndex)
	{
		return this.rotations[boneIndex];
	}

	/**
	 * Performs the scale operation.
	 * @param boneIndex the bone index to use.
	 * @return the value produced by this operation.
	 */
	public Vector3f scale(int boneIndex)
	{
		return this.scales[boneIndex];
	}

	/**
	 * Performs the scalar channel operation.
	 * @param channelIndex the channel index to use.
	 * @return the value produced by this operation.
	 */
	public float scalarChannel(int channelIndex)
	{
		return this.scalarChannels[channelIndex];
	}

	/**
	 * Performs the scalar channel operation.
	 * @param channelIndex the channel index to use.
	 * @param value the value to use.
	 */
	public void scalarChannel(int channelIndex, float value)
	{
		this.scalarChannels[channelIndex] = value;
	}

	/**
	 * Determines whether dirty.
	 * @param boneIndex the bone index to use.
	 * @return the value produced by this operation.
	 */
	public boolean isDirty(int boneIndex)
	{
		return this.dirtyBones.get(boneIndex);
	}

	/**
	 * Determines whether valid.
	 * @param boneIndex the bone index to use.
	 * @return the value produced by this operation.
	 */
	public boolean isValid(int boneIndex)
	{
		return this.validBones.get(boneIndex);
	}

	/**
	 * Performs the dirty bones operation.
	 * @return the value produced by this operation.
	 */
	public BitSet dirtyBones()
	{
		return (BitSet) this.dirtyBones.clone();
	}

	/**
	 * Performs the valid bones operation.
	 * @return the value produced by this operation.
	 */
	public BitSet validBones()
	{
		return (BitSet) this.validBones.clone();
	}

	/**
	 * Performs the set operation.
	 * @param boneIndex the bone index to use.
	 * @param translation the translation to use.
	 * @param rotation the rotation to use.
	 * @param scale the scale to use.
	 */
	public void set(int boneIndex, Vector3f translation, Quaternionf rotation, Vector3f scale)
	{
		this.translations[boneIndex].set(translation);
		this.rotations[boneIndex].set(rotation);
		this.scales[boneIndex].set(scale);
		this.validBones.set(boneIndex);
	}
	
	/**
	 * Sets the animated.
	 * @param boneIndex the bone index to use.
	 * @param translation the translation to use.
	 * @param rotation the rotation to use.
	 * @param scale the scale to use.
	 */
	public void setAnimated(int boneIndex, Vector3f translation, Quaternionf rotation, Vector3f scale)
	{
		set(boneIndex, translation, rotation, scale);
		this.dirtyBones.set(boneIndex);
	}

	/**
	 * Performs the translation operation.
	 * @param boneIndex the bone index to use.
	 * @param value the value to use.
	 */
	@Override
	public void translation(int boneIndex, Vector3f value)
	{
		this.translations[boneIndex].set(value);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}

	/**
	 * Performs the rotation operation.
	 * @param boneIndex the bone index to use.
	 * @param value the value to use.
	 */
	@Override
	public void rotation(int boneIndex, Quaternionf value)
	{
		this.rotations[boneIndex].set(value);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}

	/**
	 * Performs the scale operation.
	 * @param boneIndex the bone index to use.
	 * @param value the value to use.
	 */
	@Override
	public void scale(int boneIndex, Vector3f value)
	{
		this.scales[boneIndex].set(value);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}
}
