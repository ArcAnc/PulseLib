/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.base.PPartialTickRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the vanilla extraction parameter for all entity render state consumers. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererExtractionMixin
{
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void pulselib$capturePartialTick(Entity entity, EntityRenderState state, float partialTick, CallbackInfo callback)
	{
		((PPartialTickRenderState)state).pulselib$setPartialTick(partialTick);
	}
}
