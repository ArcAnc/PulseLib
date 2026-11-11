/**
 * @author ArcAnc
 * Created at: 15.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Groups accessors for native render setup and transparency classification.
 */
public interface RenderTypeAccessor
{
	/** Exposes the native pipeline held by a render setup. */
	@Mixin(RenderSetup.class)
	interface CompositeStateAccessor
	{
		/** Returns the actual native pipeline containing this setup's blend state. */
		@Accessor("pipeline")
		RenderPipeline pulselib$getTransparencyState();
	}

	/** Exposes the native state owned by a render type. */
	@Mixin(RenderType.class)
	interface CompositeRenderTypeAccessor
	{
		/** Returns the render setup owned by this render type. */
		@Accessor("state")
		RenderSetup pulselib$getState();
	}
}
