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
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAttachmentEligibility;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderState;
import com.arcanc.pulselib.content.player.deformer.PPlayerGeometryRenderer;
import com.arcanc.pulselib.content.renderer.PWorldRenderEligibility;
import com.arcanc.pulselib.content.renderer.base.PModelPartSnapshot;
import com.arcanc.pulselib.content.renderer.base.PPartialTickRenderState;
import com.arcanc.pulselib.content.renderer.base.PPlayerRigRenderState;
import com.arcanc.pulselib.content.renderer.base.PPlayerRootRenderState;
import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelSnapshots;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
		try (var caller = entity instanceof Player && (Object)this instanceof LivingEntityRenderer<?, ?, ?> renderer ?
				PModelPartSnapshot.capture(renderer.getModel().root()) : null)
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
				{
					PPlayerRigPose pose = entity instanceof Player player ? PPlayerRigPose.capture(player, partialTick) : null;
					rig.pulselib$setPlayerRig(pose);
					if (pose != null && !pose.isEmpty())
						PWorldRenderEligibility.markGeometry();
				}
				if (state instanceof PPlayerGeometryRenderState geometry)
				{
					geometry.pulselib$setPlayerGeometry(null);
					geometry.pulselib$setSubmittingGeometry(null);
					if (state instanceof AvatarRenderState avatar && entity instanceof Player player && (Object)this instanceof PPlayerGeometryRenderer renderer)
					{
						var shapes = renderer.pulselib$captureGeometry(player, avatar, partialTick);
						geometry.pulselib$setPlayerGeometry(shapes);
						if (shapes.hasArmor())
							PWorldRenderEligibility.markGeometry();
					}
				}
				if (entity instanceof AbstractClientPlayer player && PPlayerAttachmentEligibility.hasRenderer(player, partialTick))
					PWorldRenderEligibility.markGeometry();
				if (entity instanceof LivingEntity living && state instanceof HumanoidRenderState humanoid && (Object)this instanceof LivingEntityRenderer<?, ?, ?> renderer)
					PArmorModelSnapshots.capture(living, humanoid, renderer, partialTick);
				return state;
			});
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void pulselib$capturePartialTick(Entity entity, EntityRenderState state, float partialTick, CallbackInfo callback)
	{
		((PPartialTickRenderState)state).pulselib$setPartialTick(partialTick);
	}
}
