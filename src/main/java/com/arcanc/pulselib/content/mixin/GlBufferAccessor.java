/**
 * @author ArcAnc
 * Created at: 14.08.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.mojang.blaze3d.opengl.GlBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlBuffer.class)
/**
 * Defines the contract for gl buffer accessor.
 */
public interface GlBufferAccessor
{
	/**
	 * Performs the pulselib$get handle operation.
	 * @return the value produced by this operation.
	 */
	@Accessor("handle")
	int pulselib$getHandle();
}
