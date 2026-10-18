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
 * Decodes typed glTF animation keyframe values from accessor buffer data.
 *
 * @param <T> the value type carried by this contract
 */
public interface PGltfChannelDecoder<T>
{
	/**
	 * Returns glTF channel target names recognized by this decoder.
	 *
	 * @return glTF channel target names recognized by this decoder
	 */
	Set<String> fieldNames();

	/**
	 * Returns the animation channel type produced by this decoder.
	 *
	 * @return the animation channel type produced by this decoder
	 */
	PAnimationChannelType<T> channel();

	/**
	 * Reads the selected glTF keyframe value and converts it into a typed animation value.
	 *
	 * @param values the packed binary data
	 * @param keyframeIndex the zero-based source keyframe index
	 * @param context the glTF source conversion and interpolation context
	 * @return the decoded constant or expression-backed channel value
	 */
	PAnimationValue<T> decodeValue(ByteBuffer values, int keyframeIndex, PGltfDecodeContext context);
}
