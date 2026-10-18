/**
 * @author ArcAnc
 * Created at: 26.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.data.gltf;

import com.arcanc.pulselib.content.model.PBone;
import com.arcanc.pulselib.content.model.animation.PAnimationDecodeContext;

/**
 * glTF source conversion and interpolation information supplied to animation channel decoders.
 *
 * @param bone the bone or bone index being processed
 */
public record PGltfDecodeContext(PBone bone) implements PAnimationDecodeContext
{
}
