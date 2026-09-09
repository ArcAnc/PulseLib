/**
 * @author ArcAnc
 * Created at: 27.05.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gltf;


import com.arcanc.pulselib.content.model.animation.PAnimation;
import com.arcanc.pulselib.data.PAnimationSidecarParser;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class PGltfAnimationEventSidecarParser
{
	public static JsonElement parseJson(InputStream stream) throws IOException
	{
		return PAnimationSidecarParser.parseJson(stream);
	}
	
	public static void mergeSidecar(JsonElement root, Map<String, PAnimation> animations)
	{
		PAnimationSidecarParser.mergeSidecar(root, animations);
	}
}
