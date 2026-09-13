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
	 * @param element the element to use.
	 * @param context the context to use.
	 * @return the value produced by this operation.
	 */
	PAnimationValue<T> decodeValue(JsonElement element, PGeckoDecodeContext context);
}
