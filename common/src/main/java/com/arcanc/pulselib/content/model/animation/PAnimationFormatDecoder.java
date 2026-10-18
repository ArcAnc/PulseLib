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
 * Decodes the animation tracks of a source-format bone node.
 */
public interface PAnimationFormatDecoder
{
	/**
	 * Decodes supported channel tracks from one bone animation node.
	 *
	 * @param context the context
	 * @param boneNode the source-format JSON node containing one bone's animation tracks
	 * @return the supported channel tracks indexed by channel name
	 */
	Map<String, PAnimationTrack<?>> decodeBoneTracks(PAnimationDecodeContext context, JsonElement boneNode);
}
