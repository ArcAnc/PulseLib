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
	private final BitSet translationChannels = new BitSet();
	private final BitSet rotationChannels = new BitSet();
	private final BitSet scaleChannels = new BitSet();
	private final BitSet validScalars = new BitSet();
	private final BitSet visibilityChannels = new BitSet();
	private final BitSet invisibleBones = new BitSet();

	public PPose(int boneCount)
	{
		this(boneCount, 0);
	}

	public PPose(int boneCount, int scalarChannelCount)
	{
		if (boneCount < 0 || scalarChannelCount < 0)
		{
			throw new IllegalArgumentException("Pose sizes must be non-negative");
		}
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

	/** Deep copy, including channel presence, scalar values, visibility and dirty/valid flags. */
	public PPose copy()
	{
		return copy(scalarChannelCount());
	}

	PPose copy(int scalarChannelCount)
	{
		PPose copy = new PPose(boneCount(), Math.max(scalarChannelCount, scalarChannelCount()));
		for (int bone = 0; bone < boneCount(); bone++)
		{
			copy.translations[bone].set(this.translations[bone]);
			copy.rotations[bone].set(this.rotations[bone]);
			copy.scales[bone].set(this.scales[bone]);
		}
		System.arraycopy(this.scalarChannels, 0, copy.scalarChannels, 0, this.scalarChannels.length);
		copy.dirtyBones.or(this.dirtyBones);
		copy.validBones.or(this.validBones);
		copy.translationChannels.or(this.translationChannels);
		copy.rotationChannels.or(this.rotationChannels);
		copy.scaleChannels.or(this.scaleChannels);
		copy.validScalars.or(this.validScalars);
		copy.visibilityChannels.or(this.visibilityChannels);
		copy.invisibleBones.or(this.invisibleBones);
		return copy;
	}

	public int scalarChannelCount()
	{
		return this.scalarChannels.length;
	}

	public boolean hasScalarChannel(int channel)
	{
		return this.validScalars.get(channel);
	}

	public boolean hasTranslation(int bone)
	{
		Vector3f value = this.translations[bone];
		return this.translationChannels.get(bone) || value.x != 0.0f || value.y != 0.0f || value.z != 0.0f;
	}

	public boolean hasRotation(int bone)
	{
		Quaternionf value = this.rotations[bone];
		return this.rotationChannels.get(bone) || value.x != 0.0f || value.y != 0.0f || value.z != 0.0f || value.w != 1.0f;
	}

	public boolean hasScale(int bone)
	{
		Vector3f value = this.scales[bone];
		return this.scaleChannels.get(bone) || value.x != 1.0f || value.y != 1.0f || value.z != 1.0f;
	}

	public boolean hasVisibility(int bone)
	{
		return this.visibilityChannels.get(bone);
	}

	public boolean isVisible(int bone)
	{
		return !this.invisibleBones.get(bone);
	}

	public void visibility(int bone, boolean visible)
	{
		this.visibilityChannels.set(bone);
		this.invisibleBones.set(bone, !visible);
	}

	void markDirty(int bone)
	{
		this.dirtyBones.set(bone);
	}

	void includeChannels(PPose source, int bone)
	{
		if (source.hasTranslation(bone))
		{
			this.translationChannels.set(bone);
		}
		if (source.hasRotation(bone))
		{
			this.rotationChannels.set(bone);
		}
		if (source.hasScale(bone))
		{
			this.scaleChannels.set(bone);
		}
		if (source.isValid(bone) || source.hasTranslation(bone) || source.hasRotation(bone) || source.hasScale(bone))
		{
			this.validBones.set(bone);
		}
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
		this.validScalars.set(channelIndex);
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
		this.translationChannels.set(boneIndex);
		this.rotationChannels.set(boneIndex);
		this.scaleChannels.set(boneIndex);
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
		this.translationChannels.set(boneIndex);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}

	@Override
	public void rotation(int boneIndex, Quaternionf value)
	{
		this.rotations[boneIndex].set(value);
		this.rotationChannels.set(boneIndex);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}

	@Override
	public void scale(int boneIndex, Vector3f value)
	{
		this.scales[boneIndex].set(value);
		this.scaleChannels.set(boneIndex);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}
}
