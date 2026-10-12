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
 * Part of this code copied from Geckolib: <a href="https://github.com/bernie-g/geckolib/blob/1.21.1/common/src/main/java/software/bernie/geckolib/renderer/GeoRenderer.java">GeoRenderer</a>
 * <p>Stop crying, Tslat!</p>
 * <p>Modified by ArcAnc</p>
 */
public interface PRenderer<T extends PAnimatable<T>>
{
	PModelData getModelData(T animatable);
	
	@Nullable PBakedModel getModel(T animatable);
	RenderType getRenderType(ResourceLocation texture);
	
	void preSubmit(PoseStack poseStack,
	               T animatable,
	               Function<ResourceLocation, RenderType> renderType,
	               MultiBufferSource bufferSource,
	               int packedLight,
	               int packedOverlay,
	               float partialTick,
	               /// This is hack created just to transfer ItemDisplayContext to other methods. Do not touch it, if you don't know, what are you doing
	               @Nullable Object... additionalData);
	void trueSubmit(PoseStack poseStack,
	                T animatable,
	                Function<ResourceLocation, RenderType> renderType,
	                MultiBufferSource bufferSource,
	                int packedLight,
	                int packedOverlay,
	                float partialTick,
	                /// This is hack created just to transfer ItemDisplayContext to other methods. Do not touch it, if you don't know, what are you doing
	                @Nullable Object... additionalData);
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
