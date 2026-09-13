/**
 * @author ArcAnc
 * Created at: 02.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockEntityRenderState.class)
public interface BlockEntityRenderStateAccessor
{
	/**
	 * Performs the pulselib$get block state operation.
	 * @return the value produced by this operation.
	 */
	@Accessor("blockState")
	BlockState pulselib$getBlockState();
}
