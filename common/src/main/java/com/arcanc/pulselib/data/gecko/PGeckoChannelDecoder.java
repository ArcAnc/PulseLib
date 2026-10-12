/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gecko;

import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationValue;
import com.google.gson.JsonElement;

import java.util.Set;

public interface PGeckoChannelDecoder<T>
{
	Set<String> fieldNames();

	PAnimationChannelType<T> channel();

	PAnimationValue<T> decodeValue(JsonElement element, PGeckoDecodeContext context);
}
