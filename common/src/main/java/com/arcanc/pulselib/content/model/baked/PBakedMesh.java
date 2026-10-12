/**
 * @author ArcAnc
 * Created at: 28.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;


import com.arcanc.pulselib.content.model.PMeshPrimitive;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PBakedMesh(
		UUID uuid,
		PGeometryData geometry,
		String textureReference,
		boolean isEmissive,
		PAlphaMode alphaMode,
		PMeshPrimitive source,
		ResourceLocation textureLocation)
{
}
