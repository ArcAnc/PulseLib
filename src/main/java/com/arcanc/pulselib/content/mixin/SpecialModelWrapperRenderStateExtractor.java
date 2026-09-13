/**
 * @author ArcAnc
 * Created at: 04.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import com.arcanc.pulselib.content.renderer.base.PItemRenderState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin (SpecialModelWrapper.class)
public class SpecialModelWrapperRenderStateExtractor<T>
{
	/**
	 * Performs the pulselib$extract render state operation.
	 * @param renderer the renderer to use.
	 * @param itemStack the item stack to use.
	 * @param original the original to use.
	 * @param output the output to use.
	 * @return the value produced by this operation.
	 */
	@WrapOperation (method = "update",
			at = @At (value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/special/SpecialModelRenderer;extractArgument(Lnet/minecraft/world/item/ItemStack;)Ljava/lang/Object;"))
	private T pulselib$extractRenderState(SpecialModelRenderer<T> renderer,
	                                      ItemStack itemStack,
	                                      Operation<T> original,
	                                      ItemStackRenderState output)
	{
		T argument = original.call(renderer, itemStack);
		
		if (argument instanceof PItemRenderState<?> renderState)
			renderState.extractItemRenderState(output);
		return argument;
	}
}
