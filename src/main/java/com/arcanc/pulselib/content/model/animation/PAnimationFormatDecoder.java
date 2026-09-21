/**
 * @author ArcAnc
 * Created at: 05.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.animation;
import com.google.gson.JsonElement;

import java.util.Map;
/**
 * Defines the contract for animation format decoder.
 */
public interface PAnimationFormatDecoder
{
	/**
	 * Decodes the bone tracks.
	 * @param context the context to use.
	 * @param boneNode the bone node to use.
	 * @return the value produced by this operation.
	 */
	Map<String, PAnimationTrack<?>> decodeBoneTracks(PAnimationDecodeContext context, JsonElement boneNode);
}
