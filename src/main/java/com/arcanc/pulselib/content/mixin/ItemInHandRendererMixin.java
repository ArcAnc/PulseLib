/**
 * @author ArcAnc
 * Created at: 23.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.animation.firstPerson.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps ItemInHandRenderer's orchestration intact and changes only final
 * spatial submissions while a single hand is being rendered.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin
{
	/**
	 * Performs the pulselib$render hands with items operation.
	 * @param partialTick the partial tick to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param player the player to use.
	 * @param packedLight the packed light to use.
	 * @param original the original to use.
	 */
	@WrapMethod(method = "submitHandsWithItems")
	private void pulselib$renderHandsWithItems(float partialTick,
	                                            PoseStack poseStack,
	                                            SubmitNodeCollector submitNodeCollector,
	                                            LocalPlayer player,
	                                            int packedLight,
	                                            Operation<Void> original)
	{
		PFirstPersonRenderContexts.beginPass(player, partialTick);
		try
		{
			original.call(partialTick, poseStack, submitNodeCollector, player, packedLight);
		}
		finally
		{
			PFirstPersonRenderContexts.endPass();
		}
	}

	/**
	 * Performs the pulselib$render extra first person geometry operation.
	 * @param partialTick the partial tick to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param player the player to use.
	 * @param packedLight the packed light to use.
	 * @param ci the ci to use.
	 */
	@Inject(method = "submitHandsWithItems", at = @At("TAIL"))
	private void pulselib$renderExtraFirstPersonGeometry(float partialTick,
	                                                     PoseStack poseStack,
	                                                     SubmitNodeCollector submitNodeCollector,
	                                                     LocalPlayer player,
	                                                     int packedLight,
	                                                     CallbackInfo ci)
	{
		var pose = PFirstPersonRenderContexts.passPresentation();
		if (pose != null)
			PPlayerFirstPersonRenderer.renderExtras(player, pose, poseStack, submitNodeCollector, packedLight, partialTick);
	}

	/**
	 * Performs the pulselib$render arm with item operation.
	 * @param player the player to use.
	 * @param partialTick the partial tick to use.
	 * @param xRot the x rot to use.
	 * @param hand the hand to use.
	 * @param attack the attack to use.
	 * @param stack the stack to use.
	 * @param inverseArmHeight the inverse arm height to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param original the original to use.
	 */
	@WrapMethod(method = "submitArmWithItem")
	private void pulselib$renderArmWithItem(AbstractClientPlayer player,
	                                        float partialTick,
	                                        float xRot,
	                                        InteractionHand hand,
	                                        float attack,
	                                        ItemStack stack,
	                                        float inverseArmHeight,
	                                        PoseStack poseStack,
	                                        SubmitNodeCollector submitNodeCollector,
	                                        int packedLight,
	                                        Operation<Void> original)
	{
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		PFirstPersonRenderContexts.push(hand, arm, stack, poseStack);
		try
		{
			original.call(player, partialTick, xRot, hand, attack, stack, inverseArmHeight, poseStack, submitNodeCollector, packedLight);
			if (hand == InteractionHand.OFF_HAND && stack.isEmpty())
				renderHeldArm(player, PFirstPersonRenderContexts.current(), poseStack, submitNodeCollector, packedLight);
		}
		finally
		{
			PFirstPersonRenderContexts.pop();
		}
	}

	/**
	 * Performs the pulselib$render right hand operation.
	 * @param renderer the renderer to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param skin the skin to use.
	 * @param sleeve the sleeve to use.
	 * @param player the player to use.
	 * @param original the original to use.
	 */
	@WrapOperation(method = "renderPlayerArm", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;ZLnet/minecraft/world/entity/Avatar;)V"))
	private void pulselib$renderRightHand(AvatarRenderer<?> renderer,
	                                     PoseStack poseStack,
	                                     SubmitNodeCollector submitNodeCollector,
	                                     int packedLight,
	                                     Identifier skin,
	                                     boolean sleeve,
	                                     Avatar player,
	                                     Operation<Void> original)
	{
		renderArm(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player, original);
	}

	/**
	 * Performs the pulselib$render left hand operation.
	 * @param renderer the renderer to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param skin the skin to use.
	 * @param sleeve the sleeve to use.
	 * @param player the player to use.
	 * @param original the original to use.
	 */
	@WrapOperation(method = "renderPlayerArm", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;ZLnet/minecraft/world/entity/Avatar;)V"))
	private void pulselib$renderLeftHand(AvatarRenderer<?> renderer,
	                                    PoseStack poseStack,
	                                    SubmitNodeCollector submitNodeCollector,
	                                    int packedLight,
	                                    Identifier skin,
	                                    boolean sleeve,
	                                    Avatar player,
	                                    Operation<Void> original)
	{
		renderArm(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player, original);
	}

	/**
	 * Renders the arm.
	 * @param renderer the renderer to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param skin the skin to use.
	 * @param sleeve the sleeve to use.
	 * @param player the player to use.
	 * @param original the original to use.
	 */
	private static void renderArm(AvatarRenderer<?> renderer,
	                              PoseStack poseStack,
	                              SubmitNodeCollector submitNodeCollector,
	                              int packedLight,
	                              Identifier skin,
	                              boolean sleeve,
	                              Avatar player,
	                              Operation<Void> original)
	{
		PFirstPersonRenderContext context = PFirstPersonRenderContexts.current();
		if (context == null || context.map())
		{
			original.call(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player);
			return;
		}
		PFirstPersonArmPose armPose = context.arm() == HumanoidArm.RIGHT ?
				context.presentation().rightArm() : context.presentation().leftArm();
		if (armPose.mode() == PFirstPersonRenderMode.HIDDEN)
			return;
		if (armPose.mode() == PFirstPersonRenderMode.VANILLA)
		{
			original.call(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player);
			return;
		}
		if (armPose.transform() == null)
			return;
		poseStack.pushPose();
		try
		{
			/* The arm render hook is only a bridge to Minecraft's geometry API.
			 * The matrix came from PFirstPersonPresentation's canonical MODEL pose. */
			PFirstPersonPoseStack.replaceLocalPose(poseStack, context.basePose(), context.baseNormal(),
					PVanillaFirstPersonArmResolver.resolveArmOrigin(context.arm(), armPose.transform()));
			original.call(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	/**
	 * Performs the pulselib$render item operation.
	 * @param renderer the renderer to use.
	 * @param entity the entity to use.
	 * @param stack the stack to use.
	 * @param displayContext the display context to use.
	 * @param poseStack the pose stack to use.
	 * @param submitNodeCollector the submit node collector to use.
	 * @param packedLight the packed light to use.
	 * @param original the original to use.
	 */
	@WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
	private void pulselib$renderItem(ItemInHandRenderer renderer,
	                                 LivingEntity entity,
	                                 ItemStack stack,
	                                 ItemDisplayContext displayContext,
	                                 PoseStack poseStack,
	                                 SubmitNodeCollector submitNodeCollector,
	                                 int packedLight,
	                                 Operation<Void> original)
	{
		PFirstPersonRenderContext context = PFirstPersonRenderContexts.current();
		if (context == null || context.map())
		{
			original.call(renderer, entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
			return;
		}
		PFirstPersonItemPose itemPose = context.arm() == HumanoidArm.RIGHT ?
				context.presentation().rightItem() : context.presentation().leftItem();
		renderHeldArm(entity, context, poseStack, submitNodeCollector, packedLight);
		if (itemPose.mode() == PFirstPersonRenderMode.HIDDEN)
			return;
		if (itemPose.mode() == PFirstPersonRenderMode.VANILLA)
		{
			original.call(renderer, entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
			return;
		}
		poseStack.pushPose();
		try
		{
			if (itemPose.transformMode() == PFirstPersonTransformMode.ADDITIVE)
				poseStack.mulPose(itemPose.transform().matrix());
			else
				PFirstPersonPoseStack.replaceLocalPose(poseStack, context.basePose(), context.baseNormal(),
						PVanillaFirstPersonItemResolver.resolveItemOrigin(context.arm(), itemPose.transform()));
			original.call(renderer, entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	/**
	 * Minecraft has no arm draw-call for a non-empty ordinary hand. Submit the
	 * existing vanilla arm renderer immediately before its final item submission,
	 * so the item pipeline itself remains untouched.
	 */
	private static void renderHeldArm(LivingEntity entity,
	                                  @Nullable PFirstPersonRenderContext context,
	                                  PoseStack poseStack,
	                                  SubmitNodeCollector submitNodeCollector,
	                                  int packedLight)
	{
		if (context == null || !(entity instanceof AbstractClientPlayer player) || player.isInvisible())
			return;
		PFirstPersonArmPose armPose = context.arm() == HumanoidArm.RIGHT ?
				context.presentation().rightArm() : context.presentation().leftArm();
		if (armPose.mode() != PFirstPersonRenderMode.ANIMATED)
			return;
		poseStack.pushPose();
		try
		{
			if (armPose.transform() == null)
				return;
			PFirstPersonPoseStack.replaceLocalPose(poseStack, context.basePose(), context.baseNormal(),
					PVanillaFirstPersonArmResolver.resolveArmOrigin(context.arm(), armPose.transform()));
			AvatarRenderer<AbstractClientPlayer> renderer = Minecraft.getInstance().
					getEntityRenderDispatcher().
					getPlayerRenderer(player);
			Identifier skin = player.getSkin().body().texturePath();
			boolean sleeve = player.isModelPartShown(context.arm() == HumanoidArm.RIGHT ?
					PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);
			if (context.arm() == HumanoidArm.RIGHT)
				renderer.renderRightHand(poseStack, submitNodeCollector, packedLight, skin, sleeve, player);
			else
				renderer.renderLeftHand(poseStack, submitNodeCollector, packedLight, skin, sleeve, player);
		}
		finally
		{
			poseStack.popPose();
		}
	}
}
