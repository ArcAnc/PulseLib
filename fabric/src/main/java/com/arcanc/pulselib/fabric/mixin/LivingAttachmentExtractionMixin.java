/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.fabric.mixin;

import com.arcanc.pulselib.util.attachments.humanoid.armor.PLibArmorHandler;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures shared attachment entries after the complete virtual native extraction on Fabric. */
@Mixin(EntityRenderer.class)
public abstract class LivingAttachmentExtractionMixin
{
	@Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
	private void pulselib$captureAttachments(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> callback)
	{
		if (entity instanceof LivingEntity living && callback.getReturnValue() instanceof LivingEntityRenderState state)
			PLibArmorHandler.extractAttachmentRenderData(living, state);
	}
}
