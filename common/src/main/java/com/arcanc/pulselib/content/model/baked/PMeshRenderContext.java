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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Inherited per-mesh rendering inputs, including texture, light, color, alpha mode and optional
 * deformation.
 *
 * @param renderType the render type
 * @param color the packed ARGB vertex color
 * @param packedLight the packed block and sky light coordinates
 * @param packedOverlay the packed overlay texture coordinates
 * @param deformation the prepared mesh deformation and its current sampled values; may be {@code null}
 * @param texture the texture resource location; may be {@code null}
 * @param emissive the emissive; may be {@code null}
 * @param alphaModeOverride the optional alpha handling override; may be {@code null}
 */
public record PMeshRenderContext(
		Function<ResourceLocation, RenderType> renderType,
		int color,
		int packedLight,
		int packedOverlay,
		@Nullable PMeshDeformation deformation,
		@Nullable ResourceLocation texture,
		@Nullable Boolean emissive,
		@Nullable PAlphaMode alphaModeOverride)
{
	/**
	 * Creates inherited per-mesh rendering inputs with optional deformation.
	 *
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 */
	public PMeshRenderContext(Function<ResourceLocation, RenderType> renderType, int color, int packedLight, int packedOverlay)
	{
		this(renderType, color, packedLight, packedOverlay, null, null, null, null);
	}

	/**
	 * Creates inherited per-mesh rendering inputs with optional deformation.
	 *
	 * @param renderType the render type
	 * @param color the packed ARGB vertex color
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param deformation the prepared mesh deformation and its current sampled values; may be {@code null}
	 */
	public PMeshRenderContext(Function<ResourceLocation, RenderType> renderType, int color, int packedLight, int packedOverlay,
	                          @Nullable PMeshDeformation deformation)
	{
		this(renderType, color, packedLight, packedOverlay, deformation, null, null, null);
	}

	/**
	 * Returns a copy with deformation.
	 *
	 * @param deformation the prepared mesh deformation and its current sampled values; may be {@code null}
	 * @return a copy with deformation
	 */
	public PMeshRenderContext withDeformation(@Nullable PMeshDeformation deformation)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, this.packedOverlay, deformation, this.texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Returns a copy with texture.
	 *
	 * @param texture the texture resource location; may be {@code null}
	 * @return a copy with texture
	 */
	public PMeshRenderContext withTexture(@Nullable ResourceLocation texture)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, this.packedOverlay, this.deformation, texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Returns a copy with emissive.
	 *
	 * @param emissive the emissive; may be {@code null}
	 * @return a copy with emissive
	 */
	public PMeshRenderContext withEmissive(@Nullable Boolean emissive)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, this.packedOverlay, this.deformation, this.texture, emissive, this.alphaModeOverride);
	}

	/**
	 * Returns a copy with color.
	 *
	 * @param color the packed ARGB vertex color
	 * @return a copy with color
	 */
	public PMeshRenderContext withColor(int color)
	{
		return new PMeshRenderContext(this.renderType, color, this.packedLight, this.packedOverlay, this.deformation, this.texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Returns a copy with packed light.
	 *
	 * @param packedLight the packed block and sky light coordinates
	 * @return a copy with packed light
	 */
	public PMeshRenderContext withPackedLight(int packedLight)
	{
		return new PMeshRenderContext(this.renderType, this.color, packedLight, this.packedOverlay, this.deformation, this.texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Returns a copy with packed overlay.
	 *
	 * @param packedOverlay the packed overlay texture coordinates
	 * @return a copy with packed overlay
	 */
	public PMeshRenderContext withPackedOverlay(int packedOverlay)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, packedOverlay, this.deformation, this.texture, this.emissive, this.alphaModeOverride);
	}

	/**
	 * Returns a copy with alpha mode.
	 *
	 * @param alphaModeOverride the optional alpha handling override; may be {@code null}
	 * @return a copy with alpha mode
	 */
	public PMeshRenderContext withAlphaMode(@Nullable PAlphaMode alphaModeOverride)
	{
		return new PMeshRenderContext(this.renderType, this.color, this.packedLight, this.packedOverlay, this.deformation, this.texture, this.emissive, alphaModeOverride);
	}
}
