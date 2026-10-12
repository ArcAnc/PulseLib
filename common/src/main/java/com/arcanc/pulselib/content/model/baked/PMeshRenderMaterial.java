/**
 * @author ArcAnc
 * Created at: 13.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;

import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.util.PRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public record PMeshRenderMaterial(PBakedMesh mesh, boolean emissive, int packedLight)
{
	public static PMeshRenderMaterial resolve(PBakedMesh mesh, PMeshRenderContext context)
	{
		PBakedMesh texturedMesh = PMeshTextureVariants.resolve(mesh, context.texture());
		boolean emissive = context.emissive() == null ? texturedMesh.isEmissive() : context.emissive();
		return new PMeshRenderMaterial(texturedMesh, emissive, emissive ? LightTexture.FULL_BRIGHT : context.packedLight());
	}

	public RenderType resolveRenderType(PMeshRenderContext context, ResourceLocation textureAtlas)
	{
		PAlphaMode override = context.alphaModeOverride();
		RenderType type;
		if (override == null)
			type = context.renderType().apply(textureAtlas);
		else
		{
			PAlphaMode resolved = override == PAlphaMode.AUTO ? this.mesh.alphaMode() : override;
			type = PRenderTypes.RenderTypeProvider.forAlphaMode(resolved, textureAtlas);
		}
		return this.emissive ? PRenderTypes.RenderTypeProvider.emissiveVariant(type, textureAtlas) : type;
	}
}
