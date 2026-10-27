/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage52PartsProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Supplies isolated recording consumers only while direct native failure probes are active. */
@Mixin(ItemFeatureRenderer.class)
public abstract class Stage52ItemBuffersMixin
{
	@WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer pulselib$main(MultiBufferSource.BufferSource source, RenderType type, Operation<VertexConsumer> original)
	{
		@Nullable VertexConsumer buffer = Stage52PartsProbe.itemBuffer("main");
		return buffer != null ? buffer : original.call(source, type);
	}

	@WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer pulselib$outline(OutlineBufferSource source, RenderType type, Operation<VertexConsumer> original)
	{
		@Nullable VertexConsumer buffer = Stage52PartsProbe.itemBuffer("outline");
		return buffer != null ? buffer : original.call(source, type);
	}

	@WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer pulselib$foil(MultiBufferSource source, RenderType type, PoseStack.@Nullable Pose pose, Operation<VertexConsumer> original)
	{
		@Nullable VertexConsumer buffer = Stage52PartsProbe.itemBuffer("foil");
		return buffer != null ? buffer : original.call(source, type, pose);
	}
}
