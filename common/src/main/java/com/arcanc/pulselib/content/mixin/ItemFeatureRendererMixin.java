/**
 * @author ArcAnc
 * Created at: 27.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Nested item draws must return the enclosing draw's color, light and overlay values. */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin
{
	@Shadow
	@Final
	private QuadInstance quadInstance;

	@WrapMethod(method = "prepareSubmit(Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;Z)V")
	private void pulselib$restoreItemValues(ItemFeatureRenderer.Submit submit, boolean foil, Operation<Void> original)
	{
		int[] colors = new int[4];
		int[] lights = new int[4];
		for (int vertex = 0; vertex < 4; vertex++)
		{
			colors[vertex] = this.quadInstance.getColor(vertex);
			lights[vertex] = this.quadInstance.getLightCoords(vertex);
		}
		int overlay = this.quadInstance.overlayCoords();
		try
		{
			this.quadInstance.setLightCoords(submit.lightCoords());
			this.quadInstance.setOverlayCoords(submit.overlayCoords());
			original.call(submit, foil);
		}
		finally
		{
			for (int vertex = 0; vertex < 4; vertex++)
			{
				this.quadInstance.setColor(vertex, colors[vertex]);
				this.quadInstance.setLightCoords(vertex, lights[vertex]);
			}
			this.quadInstance.setOverlayCoords(overlay);
		}
	}

	/** Foil preparation is a separate batch in 26.2 and must not inherit another item's last main-pass tint. */
	@WrapOperation(method = "prepareFoilSubmit(Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
	private void pulselib$foilValues(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, QuadInstance instance,
			Operation<Void> original, @Local(argsOnly = true) ItemFeatureRenderer.Submit submit)
	{
		int index = quad.materialInfo().tintIndex();
		instance.setColor(quad.materialInfo().isTinted() && index >= 0 && index < submit.tintLayers().length ? submit.tintLayers()[index] : -1);
		original.call(consumer, pose, quad, instance);
	}
}
