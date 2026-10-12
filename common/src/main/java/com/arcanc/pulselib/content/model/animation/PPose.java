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

	public PPose(int boneCount)
	{
		this(boneCount, 0);
	}

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

	public int boneCount()
	{
		return this.translations.length;
	}

	public Vector3f translation(int boneIndex)
	{
		return this.translations[boneIndex];
	}

	public Quaternionf rotation(int boneIndex)
	{
		return this.rotations[boneIndex];
	}

	public Vector3f scale(int boneIndex)
	{
		return this.scales[boneIndex];
	}

	public float scalarChannel(int channelIndex)
	{
		return this.scalarChannels[channelIndex];
	}

	public void scalarChannel(int channelIndex, float value)
	{
		this.scalarChannels[channelIndex] = value;
	}

	public boolean isDirty(int boneIndex)
	{
		return this.dirtyBones.get(boneIndex);
	}

	public boolean isValid(int boneIndex)
	{
		return this.validBones.get(boneIndex);
	}

	public BitSet dirtyBones()
	{
		return (BitSet) this.dirtyBones.clone();
	}

	public BitSet validBones()
	{
		return (BitSet) this.validBones.clone();
	}

	public void set(int boneIndex, Vector3f translation, Quaternionf rotation, Vector3f scale)
	{
		this.translations[boneIndex].set(translation);
		this.rotations[boneIndex].set(rotation);
		this.scales[boneIndex].set(scale);
		this.validBones.set(boneIndex);
	}
	
	public void setAnimated(int boneIndex, Vector3f translation, Quaternionf rotation, Vector3f scale)
	{
		set(boneIndex, translation, rotation, scale);
		this.dirtyBones.set(boneIndex);
	}

	@Override
	public void translation(int boneIndex, Vector3f value)
	{
		this.translations[boneIndex].set(value);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}

	@Override
	public void rotation(int boneIndex, Quaternionf value)
	{
		this.rotations[boneIndex].set(value);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}

	@Override
	public void scale(int boneIndex, Vector3f value)
	{
		this.scales[boneIndex].set(value);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}
}
