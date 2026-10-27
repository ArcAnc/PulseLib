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
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Nested item draws must return the enclosing draw's color, light and overlay values. */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin
{
	@Shadow
	@Final
	private QuadInstance quadInstance;

	@WrapMethod(method = "renderItem")
	private void pulselib$restoreItemValues(MultiBufferSource.BufferSource buffers, OutlineBufferSource outlines,
			SubmitNodeStorage.ItemSubmit submit, Operation<Void> original)
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
			original.call(buffers, outlines, submit);
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
}
