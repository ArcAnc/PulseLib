/**
 * @author ArcAnc
 * Created at: 04.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
/**
 * Defines the contract for item stack render state accessor.
 */
public interface ItemStackRenderStateAccessor
{
	/**
	 * Performs the pulselib$get display context operation.
	 * @return the value produced by this operation.
	 */
	@Accessor ("displayContext")
	ItemDisplayContext pulselib$getDisplayContext();
}
