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

/**
 * Render-ready mesh pairing triangle geometry, atlas texture and material properties.
 *
 * @param uuid the stable source-model bone identity
 * @param geometry the immutable indexed mesh geometry
 * @param textureReference the source-model texture reference used for override lookup
 * @param isEmissive the is emissive
 * @param alphaMode the material alpha handling policy
 * @param source the source
 * @param textureLocation the texture location
 */
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
