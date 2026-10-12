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

public interface PGltfChannelDecoder<T>
{
	Set<String> fieldNames();

	PAnimationChannelType<T> channel();

	PAnimationValue<T> decodeValue(ByteBuffer values, int keyframeIndex, PGltfDecodeContext context);
}
