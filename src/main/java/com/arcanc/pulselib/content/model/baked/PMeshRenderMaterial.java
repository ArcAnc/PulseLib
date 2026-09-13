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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/**
 * Immutable value object representing mesh render material.
 */
public record PMeshRenderMaterial(PBakedMesh mesh, boolean emissive, int packedLight)
{
	/**
	 * Performs the resolve operation.
	 * @param mesh the mesh to use.
	 * @param context the context to use.
	 * @return the value produced by this operation.
	 */
	public static PMeshRenderMaterial resolve(PBakedMesh mesh, PMeshRenderContext context)
	{
		PBakedMesh texturedMesh = PMeshTextureVariants.resolve(mesh, context.texture());
		boolean emissive = context.emissive() == null ? texturedMesh.isEmissive() : context.emissive();
		return new PMeshRenderMaterial(texturedMesh, emissive,
				emissive ? LightCoordsUtil.FULL_BRIGHT : context.packedLight());
	}

	/**
	 * Resolves the render type.
	 * @param context the context to use.
	 * @param textureAtlas the texture atlas to use.
	 * @return the value produced by this operation.
	 */
	public RenderType resolveRenderType(PMeshRenderContext context, Identifier textureAtlas)
	{
		RenderType type = resolveBaseRenderType(context, textureAtlas, false);
		return this.emissive ? PRenderTypes.RenderTypeProvider.emissiveVariant(type, textureAtlas) : type;
	}

	/**
	 * Resolves the instant render type.
	 * @param context the context to use.
	 * @param textureAtlas the texture atlas to use.
	 * @return the value produced by this operation.
	 */
	public RenderType resolveInstantRenderType(PMeshRenderContext context, Identifier textureAtlas)
	{
		RenderType type = resolveBaseRenderType(context, textureAtlas, true);
		return this.emissive ? PRenderTypes.RenderTypeProvider.instantEmissiveVariant(type, textureAtlas) :
				PRenderTypes.RenderTypeProvider.instantVariant(type, textureAtlas);
	}

	/**
	 * Resolves the base render type.
	 * @param context the context to use.
	 * @param textureAtlas the texture atlas to use.
	 * @param instant the instant to use.
	 * @return the value produced by this operation.
	 */
	private RenderType resolveBaseRenderType(PMeshRenderContext context, Identifier textureAtlas, boolean instant)
	{
		PAlphaMode override = context.alphaModeOverride();
		if (override == null)
			return context.renderType().apply(textureAtlas);
		PAlphaMode resolved = override == PAlphaMode.AUTO ? this.mesh.alphaMode() : override;
		return instant ? PRenderTypes.RenderTypeProvider.forInstantAlphaMode(resolved, textureAtlas) :
				PRenderTypes.RenderTypeProvider.forAlphaMode(resolved, textureAtlas);
	}
}
