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

/**
 * Mutable indexed bone pose with per-channel validity and dirty masks plus optional scalar animation
 * channels.
 */
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

	/**
	 * Allocates neutral bone transforms and empty contribution masks for the requested pose sizes.
	 *
	 * @param boneCount the number of indexed bones
	 */
	public PPose(int boneCount)
	{
		this(boneCount, 0);
	}

	/**
	 * Allocates neutral bone transforms and empty contribution masks for the requested pose sizes.
	 *
	 * @param boneCount the number of indexed bones
	 * @param scalarChannelCount the number of scalar animation channels
	 */
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

	/**
	 * Deep copy, including channel presence, scalar values, visibility and dirty/valid flags.
	 *
	 * @return the indexed bone pose
	 */
	public PPose copy()
	{
		return copy(scalarChannelCount());
	}

	/**
	 * Creates an independent pose copy, optionally changing scalar-channel capacity.
	 *
	 * @param scalarChannelCount the number of scalar animation channels
	 * @return the indexed bone pose
	 */
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

	/**
	 * Returns the number of scalar channels stored by this pose.
	 *
	 * @return the number of scalar channels stored by this pose
	 */
	public int scalarChannelCount()
	{
		return this.scalarChannels.length;
	}

	/**
	 * Returns whether the scalar channel has a contributed value.
	 *
	 * @param channel the zero-based scalar channel index
	 * @return whether the scalar channel has a contributed value
	 */
	public boolean hasScalarChannel(int channel)
	{
		return this.validScalars.get(channel);
	}

	/**
	 * Returns whether the bone has an explicitly contributed translation.
	 *
	 * @param bone the zero-based baked-model bone index
	 * @return whether the bone has an explicitly contributed translation
	 */
	public boolean hasTranslation(int bone)
	{
		Vector3f value = this.translations[bone];
		return this.translationChannels.get(bone) || value.x != 0.0f || value.y != 0.0f || value.z != 0.0f;
	}

	/**
	 * Returns whether the bone has an explicitly contributed rotation.
	 *
	 * @param bone the zero-based baked-model bone index
	 * @return whether the bone has an explicitly contributed rotation
	 */
	public boolean hasRotation(int bone)
	{
		Quaternionf value = this.rotations[bone];
		return this.rotationChannels.get(bone) || value.x != 0.0f || value.y != 0.0f || value.z != 0.0f || value.w != 1.0f;
	}

	/**
	 * Returns whether the bone has an explicitly contributed scale.
	 *
	 * @param bone the zero-based baked-model bone index
	 * @return whether the bone has an explicitly contributed scale
	 */
	public boolean hasScale(int bone)
	{
		Vector3f value = this.scales[bone];
		return this.scaleChannels.get(bone) || value.x != 1.0f || value.y != 1.0f || value.z != 1.0f;
	}

	/**
	 * Returns whether the bone has an explicitly contributed visibility value.
	 *
	 * @param bone the zero-based baked-model bone index
	 * @return whether the bone has an explicitly contributed visibility value
	 */
	public boolean hasVisibility(int bone)
	{
		return this.visibilityChannels.get(bone);
	}

	/**
	 * Returns the sampled visibility of the bone, defaulting to visible when no visibility value was supplied.
	 *
	 * @param bone the zero-based baked-model bone index
	 * @return the sampled visibility of the bone, defaulting to visible when no visibility value was supplied
	 */
	public boolean isVisible(int bone)
	{
		return !this.invisibleBones.get(bone);
	}

	/**
	 * Sets the bone visibility and records its visibility contribution.
	 *
	 * @param bone the zero-based baked-model bone index
	 * @param visible whether the selected bone is visible
	 */
	public void visibility(int bone, boolean visible)
	{
		this.visibilityChannels.set(bone);
		this.invisibleBones.set(bone, !visible);
	}

	/**
	 * Marks a bone as changed so dependent transforms can be recomputed.
	 *
	 * @param bone the zero-based baked-model bone index
	 */
	void markDirty(int bone)
	{
		this.dirtyBones.set(bone);
	}

	/**
	 * Includes the source pose's channel contribution flags for the selected bone.
	 *
	 * @param source the pose supplying transform and channel-presence data
	 * @param bone the zero-based baked-model bone index
	 */
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

	/**
	 * Returns the number of indexed bones stored in this pose.
	 *
	 * @return the number of indexed bones stored in this pose
	 */
	public int boneCount()
	{
		return this.translations.length;
	}

	/**
	 * Reads or writes the translation channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @return the three-component vector
	 */
	public Vector3f translation(int boneIndex)
	{
		return this.translations[boneIndex];
	}

	/**
	 * Reads or writes the rotation channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @return the quaternion rotation
	 */
	public Quaternionf rotation(int boneIndex)
	{
		return this.rotations[boneIndex];
	}

	/**
	 * Reads or writes the scale channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @return the three-component vector
	 */
	public Vector3f scale(int boneIndex)
	{
		return this.scales[boneIndex];
	}

	/**
	 * Reads or writes a scalar animation channel.
	 *
	 * @param channelIndex the zero-based scalar channel index
	 * @return the selected scalar channel value
	 */
	public float scalarChannel(int channelIndex)
	{
		return this.scalarChannels[channelIndex];
	}

	/**
	 * Reads or writes a scalar animation channel.
	 *
	 * @param channelIndex the zero-based scalar channel index
	 * @param value the value written by this operation
	 */
	public void scalarChannel(int channelIndex, float value)
	{
		this.scalarChannels[channelIndex] = value;
		this.validScalars.set(channelIndex);
	}

	/**
	 * Returns whether the selected bone has changed.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @return whether the selected bone has changed
	 */
	public boolean isDirty(int boneIndex)
	{
		return this.dirtyBones.get(boneIndex);
	}

	/**
	 * Returns whether the selected bone has a contributed transform.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @return whether the selected bone has a contributed transform
	 */
	public boolean isValid(int boneIndex)
	{
		return this.validBones.get(boneIndex);
	}

	/**
	 * Returns the mask of bones requiring transform updates.
	 *
	 * @return the mask of bones requiring transform updates
	 */
	public BitSet dirtyBones()
	{
		return (BitSet) this.dirtyBones.clone();
	}

	/**
	 * Returns the mask of bones with contributed transforms.
	 *
	 * @return the mask of bones with contributed transforms
	 */
	public BitSet validBones()
	{
		return (BitSet) this.validBones.clone();
	}

	/**
	 * Stores the selected bone's translation, rotation and scale.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param translation the local translation vector
	 * @param rotation the local quaternion rotation
	 * @param scale the local scale vector
	 */
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
	
	/**
	 * Stores a bone transform and marks its animation channels as contributed.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param translation the local translation vector
	 * @param rotation the local quaternion rotation
	 * @param scale the local scale vector
	 */
	public void setAnimated(int boneIndex, Vector3f translation, Quaternionf rotation, Vector3f scale)
	{
		set(boneIndex, translation, rotation, scale);
		this.dirtyBones.set(boneIndex);
	}

	/**
	 * Reads or writes the translation channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param value the new local translation value
	 */
	@Override
	public void translation(int boneIndex, Vector3f value)
	{
		this.translations[boneIndex].set(value);
		this.translationChannels.set(boneIndex);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}

	/**
	 * Reads or writes the rotation channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param value the new local rotation value
	 */
	@Override
	public void rotation(int boneIndex, Quaternionf value)
	{
		this.rotations[boneIndex].set(value);
		this.rotationChannels.set(boneIndex);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}

	/**
	 * Reads or writes the scale channel of the selected bone.
	 *
	 * @param boneIndex the zero-based baked-model bone index
	 * @param value the new local scale value
	 */
	@Override
	public void scale(int boneIndex, Vector3f value)
	{
		this.scales[boneIndex].set(value);
		this.scaleChannels.set(boneIndex);
		this.validBones.set(boneIndex);
		this.dirtyBones.set(boneIndex);
	}
}
