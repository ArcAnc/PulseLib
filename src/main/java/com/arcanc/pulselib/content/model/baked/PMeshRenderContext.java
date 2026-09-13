/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.model.baked;

import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public record PMeshRenderContext(
		Function<Identifier, RenderType> renderType,
		int color,
		int packedLight,
		int packedOverlay,
		@Nullable PMeshDeformation deformation,
		@Nullable Identifier texture,
		@Nullable Boolean emissive,
		@Nullable PAlphaMode alphaModeOverride)
{
	/**
	 * Creates an instance of the enclosing type.
	 * @param renderType the render type to use.
	 * @param color the color to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 */
	public PMeshRenderContext(Function<Identifier, RenderType> renderType, int color, int packedLight, int packedOverlay)
	{
		this(renderType, color, packedLight, packedOverlay, null, null, null, null);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param renderType the render type to use.
	 * @param color the color to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 * @param deformation the deformation to use.
	 */
	public PMeshRenderContext(Function<Identifier, RenderType> renderType, int color, int packedLight, int packedOverlay,
	                          @Nullable PMeshDeformation deformation)
	{
		this(renderType, color, packedLight, packedOverlay, deformation, null, null, null);
	}

	/**
	 * Creates an instance of the enclosing type.
	 * @param renderType the render type to use.
	 * @param color the color to use.
	 * @param packedLight the packed light to use.
	 * @param packedOverlay the packed overlay to use.
	 * @param deformation the deformation to use.
	 * @param texture the texture to use.
	 * @param emissive the emissive to use.
	 */
	public PMeshRenderContext(Function<Identifier, RenderType> renderType, int color, int packedLight, int packedOverlay,
	                          @Nullable PMeshDeformation deformation, @Nullable Identifier texture, @Nullable Boolean emissive)
	{
		this(renderType, color, packedLight, packedOverlay, deformation, texture, emissive, null);
	}

	/**
	 * Performs the with deformation operation.
	 * @param deformation the deformation to use.
	 * @return the value produced by this operation.
	 */
	public PMeshRenderContext withDeformation(@Nullable PMeshDeformation deformation)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, this.packedOverlay, deformation, this.texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Performs the with texture operation.
	 * @param texture the texture to use.
	 * @return the value produced by this operation.
	 */
	public PMeshRenderContext withTexture(@Nullable Identifier texture)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, this.packedOverlay, this.deformation, texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Performs the with emissive operation.
	 * @param emissive the emissive to use.
	 * @return the value produced by this operation.
	 */
	public PMeshRenderContext withEmissive(@Nullable Boolean emissive)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, this.packedOverlay, this.deformation, this.texture, emissive, this.alphaModeOverride);
	}

	/**
	 * Performs the with color operation.
	 * @param color the color to use.
	 * @return the value produced by this operation.
	 */
	public PMeshRenderContext withColor(int color)
	{
		return new PMeshRenderContext(this.renderType, color, this.packedLight, this.packedOverlay, this.deformation, this.texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Performs the with packed light operation.
	 * @param packedLight the packed light to use.
	 * @return the value produced by this operation.
	 */
	public PMeshRenderContext withPackedLight(int packedLight)
	{
		return new PMeshRenderContext(this.renderType, this.color, packedLight, this.packedOverlay, this.deformation, this.texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Performs the with packed overlay operation.
	 * @param packedOverlay the packed overlay to use.
	 * @return the value produced by this operation.
	 */
	public PMeshRenderContext withPackedOverlay(int packedOverlay)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, packedOverlay, this.deformation, this.texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Performs the with alpha mode operation.
	 * @param alphaModeOverride the alpha mode override to use.
	 * @return the value produced by this operation.
	 */
	public PMeshRenderContext withAlphaMode(@Nullable PAlphaMode alphaModeOverride)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, this.packedOverlay, this.deformation, this.texture, this.emissive, alphaModeOverride);
	}
}
