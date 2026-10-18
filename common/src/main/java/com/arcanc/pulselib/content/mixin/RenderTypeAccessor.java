/**
 * @author ArcAnc
 * Created at: 15.03.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


/**
 * Groups accessors for vanilla composite render state and transparency classification.
 */
public interface RenderTypeAccessor
{
	/**
	 * Exposes the transparency shard of a vanilla composite render state.
	 */
	@Mixin(RenderType.CompositeState.class)
	interface CompositeStateAccessor
	{
		/**
		 * Returns the transparency state configured by this composite render state.
		 *
		 * @return the transparency state configured by this composite render state
		 */
		@Accessor ("transparencyState")
		RenderStateShard.TransparencyStateShard pulselib$getTransparencyState();
	}
	
	/**
	 * Exposes the composite state owned by a vanilla composite render type.
	 */
	@Mixin (targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
	interface CompositeRenderTypeAccessor
	{
		/**
		 * Returns the composite render state owned by this render type.
		 *
		 * @return the composite render state owned by this render type
		 */
		@Accessor ("state")
		RenderType.CompositeState pulselib$getState();
	}
}
