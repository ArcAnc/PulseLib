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

/**
 * Decodes a supported Gecko animation channel into typed keyframe values.
 *
 * @param <T> the value type carried by this contract
 */
public interface PGeckoChannelDecoder<T>
{
	/**
	 * Returns source field names recognized by this decoder.
	 *
	 * @return source field names recognized by this decoder
	 */
	Set<String> fieldNames();

	/**
	 * Returns the animation channel type produced by this decoder.
	 *
	 * @return the animation channel type produced by this decoder
	 */
	PAnimationChannelType<T> channel();

	/**
	 * Decodes a source value using the current Gecko animation context.
	 *
	 * @param element the source JSON channel value
	 * @param context the Gecko source conversion and interpolation context
	 * @return the decoded constant or expression-backed channel value
	 */
	PAnimationValue<T> decodeValue(JsonElement element, PGeckoDecodeContext context);
}
