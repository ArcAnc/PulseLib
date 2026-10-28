/**
 * @author ArcAnc
 * Created at: 28.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1.mixin;

import com.arcanc.pulselib.test.stage1.Stage52ArmorProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recording is confined to isolated feature tests; normal world buffers remain native. */
@Mixin(ModelFeatureRenderer.class)
public abstract class Stage52ArmorBuffersMixin
{
	@WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer pulselib$outline(OutlineBufferSource source, RenderType type, Operation<VertexConsumer> original)
	{
		var buffer = Stage52ArmorProbe.buffer("outline");
		return buffer == null ? original.call(source, type) : buffer;
	}

	@WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer pulselib$crumbling(MultiBufferSource.BufferSource source, RenderType type, Operation<VertexConsumer> original)
	{
		var buffer = Stage52ArmorProbe.buffer("crumbling");
		return buffer == null ? original.call(source, type) : buffer;
	}

	@Inject(method = "renderModel", at = @At("TAIL"))
	private void pulselib$world(SubmitNodeStorage.ModelSubmit<?> node, RenderType type, VertexConsumer buffer,
			OutlineBufferSource outlines, MultiBufferSource.BufferSource crumbling, CallbackInfo callback)
	{
		Stage52ArmorProbe.world(node, type);
	}
}
