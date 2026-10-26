/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.renderer.PWorldRenderEligibility;
import com.arcanc.pulselib.content.renderer.base.PPartialTickRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
	/** Encloses the complete virtual extraction, including subclass item/layer extraction. */
	@WrapMethod(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
	private EntityRenderState pulselib$captureWorldItems(Entity entity, float partialTick, Operation<EntityRenderState> original)
	{
		return PWorldRenderEligibility.capture(() -> original.call(entity, partialTick));
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void pulselib$capturePartialTick(Entity entity, EntityRenderState state, float partialTick, CallbackInfo callback)
	{
		((PPartialTickRenderState)state).pulselib$setPartialTick(partialTick);
	}
}
