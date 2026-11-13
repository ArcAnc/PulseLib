/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.mojang.blaze3d.opengl.FrameBufferCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Legacy GL wrappers borrow the real device cache; the universal RHI executor does not use this hook. */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public interface GlDeviceFrameBufferAccessor
{
	@Accessor("frameBufferCache")
	FrameBufferCache pulselib$getFrameBufferCache();
}
