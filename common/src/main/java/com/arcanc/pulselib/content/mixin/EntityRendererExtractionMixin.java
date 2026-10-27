/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerRigPose;
import com.arcanc.pulselib.content.renderer.PWorldRenderEligibility;
import com.arcanc.pulselib.content.renderer.base.PPartialTickRenderState;
import com.arcanc.pulselib.content.renderer.base.PPlayerRootRenderState;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the vanilla extraction parameter for all entity render state consumers.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererExtractionMixin
{
	/**
	 * Encloses the complete virtual extraction, including subclass item/layer extraction.
	 */
	@WrapMethod(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
	private EntityRenderState pulselib$captureWorldItems(Entity entity, float partialTick, Operation<EntityRenderState> original)
	{
		return PWorldRenderEligibility.capture(() ->
		{
			EntityRenderState state = original.call(entity, partialTick);
			if (state instanceof PPlayerRootRenderState snapshot)
			{
				snapshot.pulselib$setPlayerRoot(null);
				if (state instanceof AvatarRenderState && entity instanceof Player player)
				{
					PoseStack root = new PoseStack();
					PPlayerAnimations.applyRoot(player, root, partialTick);
					snapshot.pulselib$setPlayerRoot(root.last().pose());
					if (!PPlayerAnimations.meshAttachmentPoses(player, partialTick).isEmpty())
						PWorldRenderEligibility.markGeometry();
				}
			}
			if (state instanceof PPlayerRigRenderState rig)
				rig.pulselib$setPlayerRig(entity instanceof Player player ? PPlayerRigPose.capture(player, partialTick) : null);
			return state;
		});
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void pulselib$capturePartialTick(Entity entity, EntityRenderState state, float partialTick, CallbackInfo callback)
	{
		((PPartialTickRenderState)state).pulselib$setPartialTick(partialTick);
	}
}
