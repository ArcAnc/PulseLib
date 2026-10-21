/**
 * @author ArcAnc
 * Created at: 04.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin (SpecialModelWrapper.class)
/**
 * Defines the contract for special model wrapper accessor.
 */
public interface SpecialModelWrapperAccessor<T>
{
	/**
	 * Performs the pulselib$get special renderer operation.
	 * @return the value produced by this operation.
	 */
	@Accessor (value = "specialRenderer")
	SpecialModelRenderer<T> pulselib$getSpecialRenderer();
}
