/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.renderer;


import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Common model lookup and submission hooks shared by block, entity and item renderers.
 * <p>
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/renderer/GeoRenderer.java">GeoRenderer</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 *
 * @param <T> the animatable owner type
 */
public interface PRenderer<T extends PAnimatable<T>>
{
	/**
	 * Returns the model selection used for the supplied animatable.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @return the model selection used for the supplied animatable
	 */
	PModelData getModelData(T animatable);
	
	/**
	 * Returns the baked model selected for the supplied animatable.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @return the baked model selected for the supplied animatable, or {@code null} when the requested model has not been loaded
	 */
	@Nullable PBakedModel getModel(T animatable);
	/**
	 * Selects the render pipeline for the supplied texture.
	 *
	 * @param texture the texture resource location
	 * @return the vanilla render pipeline
	 */
	RenderType getRenderType(ResourceLocation texture);
	
	/**
	 * Runs the customization hook before this animatable's model geometry is submitted.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param animatable the owner whose animation state is being processed
	 * @param renderType the render type
	 * @param bufferSource the vanilla vertex buffer source
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param additionalData the renderer-specific values passed between submission hooks; may be {@code null}
	 */
	void preSubmit(PoseStack poseStack,
	               T animatable,
	               Function<ResourceLocation, RenderType> renderType,
	               MultiBufferSource bufferSource,
	               int packedLight,
	               int packedOverlay,
	               float partialTick,
	               /// This is hack created just to transfer ItemDisplayContext to other methods. Do not touch it, if you don't know, what are you doing
	               @Nullable Object... additionalData);
	/**
	 * Submits this animatable's model geometry using the current render inputs.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param animatable the owner whose animation state is being processed
	 * @param renderType the render type
	 * @param bufferSource the vanilla vertex buffer source
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param additionalData the renderer-specific values passed between submission hooks; may be {@code null}
	 */
	void trueSubmit(PoseStack poseStack,
	                T animatable,
	                Function<ResourceLocation, RenderType> renderType,
	                MultiBufferSource bufferSource,
	                int packedLight,
	                int packedOverlay,
	                float partialTick,
	                /// This is hack created just to transfer ItemDisplayContext to other methods. Do not touch it, if you don't know, what are you doing
	                @Nullable Object... additionalData);
	/**
	 * Runs the customization hook after this animatable's model geometry has been submitted.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param animatable the owner whose animation state is being processed
	 * @param renderType the render type
	 * @param bufferSource the vanilla vertex buffer source
	 * @param packedLight the packed block and sky light coordinates
	 * @param packedOverlay the packed overlay texture coordinates
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param additionalData the renderer-specific values passed between submission hooks; may be {@code null}
	 */
	void postSubmit(PoseStack poseStack,
	                T animatable,
	                Function<ResourceLocation, RenderType> renderType,
	                MultiBufferSource bufferSource,
	                int packedLight,
	                int packedOverlay,
	                float partialTick,
	                /// This is hack created just to transfer ItemDisplayContext to other methods. Do not touch it, if you don't know, what are you doing
	                @Nullable Object... additionalData);
}
