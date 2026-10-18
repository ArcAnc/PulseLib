/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes living renderer layer registration to shared client integration.
 */
@Mixin (LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor
{
	/**
	 * Adds a render layer to the vanilla living renderer and reports whether it was accepted.
	 *
	 * @param layer the layer contribution being blended
	 * @return whether it was accepted
	 */
	@Invoker ("addLayer")
	boolean pulselib$addLayer(RenderLayer<?, ?> layer);
}
