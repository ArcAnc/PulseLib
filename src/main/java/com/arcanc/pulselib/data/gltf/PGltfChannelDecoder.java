/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gltf;

import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationValue;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Defines the contract for gltf channel decoder.
 */
public interface PGltfChannelDecoder<T>
{
	/**
	 * Performs the field names operation.
	 * @return the value produced by this operation.
	 */
	Set<String> fieldNames();

	/**
	 * Performs the channel operation.
	 * @return the value produced by this operation.
	 */
	PAnimationChannelType<T> channel();

	/**
	 * Decodes the value.
	 * @param values the values to use.
	 * @param keyframeIndex the keyframe index to use.
	 * @param context the context to use.
	 * @return the value produced by this operation.
	 */
	PAnimationValue<T> decodeValue(ByteBuffer values, int keyframeIndex, PGltfDecodeContext context);
}
