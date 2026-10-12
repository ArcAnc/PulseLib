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


public interface RenderTypeAccessor
{
	@Mixin(RenderType.CompositeState.class)
	interface CompositeStateAccessor
	{
		@Accessor ("transparencyState")
		RenderStateShard.TransparencyStateShard pulselib$getTransparencyState();
	}
	
	@Mixin (targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
	interface CompositeRenderTypeAccessor
	{
		@Accessor ("state")
		RenderType.CompositeState pulselib$getState();
	}
}
