/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62deformation.mixin;

import com.arcanc.pulselib.test.stage62deformation.NativeParityProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Supplies isolated recording consumers only while direct native failure probes are active. */
@Mixin(ModelPartFeatureRenderer.class)
public abstract class PartBuffersMixin
{
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0))
	private VertexConsumer pulselib$main(MultiBufferSource.BufferSource source, RenderType type, Operation<VertexConsumer> original)
	{
		@Nullable VertexConsumer buffer = NativeParityProbe.buffer("main");
		return buffer != null ? buffer : original.call(source, type);
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 1))
	private VertexConsumer pulselib$crumbling(MultiBufferSource.BufferSource source, RenderType type, Operation<VertexConsumer> original)
	{
		@Nullable VertexConsumer buffer = NativeParityProbe.buffer("crumbling");
		return buffer != null ? buffer : original.call(source, type);
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer pulselib$outline(OutlineBufferSource source, RenderType type, Operation<VertexConsumer> original)
	{
		@Nullable VertexConsumer buffer = NativeParityProbe.buffer("outline");
		return buffer != null ? buffer : original.call(source, type);
	}

}
