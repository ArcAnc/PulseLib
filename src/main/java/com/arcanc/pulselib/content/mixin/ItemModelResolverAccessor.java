/**
 * @author ArcAnc
 * Created at: 04.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin (ItemModelResolver.class)
public interface ItemModelResolverAccessor
{
	/**
	 * Performs the pulselib$get model manager operation.
	 * @return the value produced by this operation.
	 */
	@Accessor (value = "modelManager")
	ModelManager pulselib$getModelManager();
}
