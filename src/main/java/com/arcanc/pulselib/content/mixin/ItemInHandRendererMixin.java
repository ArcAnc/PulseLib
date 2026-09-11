/**
 * @author ArcAnc
 * Created at: 30.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.firstPerson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin
{
	private static final ThreadLocal<FirstPersonRenderContext> PULSELIB$FIRST_PERSON_CONTEXT = new ThreadLocal<>();
	private static final ThreadLocal<InteractionHand> PULSELIB$RENDERED_HAND = new ThreadLocal<>();

	@Inject(
			method = "renderHandsWithItems",
			at = @At("HEAD"))
	private void pulselib$beginFirstPersonRender(
			float partialTick,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			LocalPlayer player,
			int packedLight,
			CallbackInfo ci)
	{
		PPlayerFirstPersonPose pose =
				PPlayerAnimations.firstPersonPose(
						player,
						partialTick);
		
		if (pose == null)
			return;

		PULSELIB$FIRST_PERSON_CONTEXT.set(new FirstPersonRenderContext(pose, player));
		PPlayerFirstPersonRenderer.renderAdditional(player, pose, poseStack, submitNodeCollector, packedLight, partialTick);
	}

	@Inject(method = "renderHandsWithItems", at = @At("RETURN"))
	private void pulselib$finishFirstPersonRender(float partialTick,
	                                               PoseStack poseStack,
	                                               SubmitNodeCollector submitNodeCollector,
	                                               LocalPlayer player,
	                                               int packedLight,
	                                               CallbackInfo ci)
	{
		FirstPersonRenderContext context = PULSELIB$FIRST_PERSON_CONTEXT.get();
		if (context == null)
			return;
		PULSELIB$FIRST_PERSON_CONTEXT.remove();
	}

	@Inject(method = "renderArmWithItem", at = @At("HEAD"))
	private void pulselib$setRenderedHand(AbstractClientPlayer player,
	                                     float partialTick,
	                                     float xRot,
	                                     InteractionHand hand,
	                                     float attack,
	                                     ItemStack itemStack,
	                                     float inverseArmHeight,
	                                     PoseStack poseStack,
	                                     SubmitNodeCollector submitNodeCollector,
	                                     int packedLight,
	                                     CallbackInfo ci)
	{
		if (PULSELIB$FIRST_PERSON_CONTEXT.get() != null)
			PULSELIB$RENDERED_HAND.set(hand);
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"))
	private void pulselib$clearRenderedHand(AbstractClientPlayer player,
	                                       float partialTick,
	                                       float xRot,
	                                       InteractionHand hand,
	                                       float attack,
	                                       ItemStack itemStack,
	                                       float inverseArmHeight,
	                                       PoseStack poseStack,
	                                       SubmitNodeCollector submitNodeCollector,
	                                       int packedLight,
	                                       CallbackInfo ci)
	{
		PULSELIB$RENDERED_HAND.remove();
	}

	@Inject(
			method = "renderArmWithItem",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
					shift = At.Shift.AFTER,
					ordinal = 0))
	private void pulselib$applyFirstPersonItemOffset(AbstractClientPlayer player,
	                                                float partialTick,
	                                                float xRot,
	                                                InteractionHand hand,
	                                                float attack,
	                                                ItemStack itemStack,
	                                                float inverseArmHeight,
	                                                PoseStack poseStack,
	                                                SubmitNodeCollector submitNodeCollector,
	                                                int packedLight,
	                                                CallbackInfo ci)
	{
		FirstPersonRenderContext context = PULSELIB$FIRST_PERSON_CONTEXT.get();
		if (context == null || itemStack.isEmpty())
			return;

		PFirstPersonItemPose itemPose = itemPose(
				context,
				itemDisplayContext(player, hand),
				hand,
				itemStack);
		if (itemPose.mode() == PFirstPersonRenderMode.ANIMATED)
		{
			Matrix4f offset = itemPose.transform();
			poseStack.translate(offset.m30(), offset.m31(), offset.m32());
		}
	}

	@Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
	private void pulselib$renderFirstPersonItem(ItemInHandRenderer renderer,
	                                            LivingEntity entity,
	                                            ItemStack stack,
	                                            ItemDisplayContext displayContext,
	                                            PoseStack poseStack,
	                                            SubmitNodeCollector submitNodeCollector,
	                                            int packedLight)
	{
		FirstPersonRenderContext context = PULSELIB$FIRST_PERSON_CONTEXT.get();
		if (context == null || !(entity instanceof LocalPlayer player))
		{
			renderer.renderItem(entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
			return;
		}

		InteractionHand hand = PULSELIB$RENDERED_HAND.get();
		PFirstPersonItemPose itemPose = itemPose(context, displayContext, hand, stack);
		switch (itemPose.mode())
		{
			case VANILLA -> renderer.renderItem(entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
			case ANIMATED -> renderer.renderItem(entity, stack, displayContext, poseStack, submitNodeCollector, packedLight);
			case HIDDEN -> { }
		}
	}

	@Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;)V"))
	private void pulselib$renderFirstPersonArm(ItemInHandRenderer renderer,
	                                           PoseStack poseStack,
	                                           SubmitNodeCollector submitNodeCollector,
	                                           int packedLight,
	                                           float inverseArmHeight,
	                                           float attackValue,
	                                           HumanoidArm arm)
	{
		FirstPersonRenderContext context = PULSELIB$FIRST_PERSON_CONTEXT.get();
		PFirstPersonArmPose armPose = context == null ? null : armPose(context.pose(), arm);
		if (armPose == null || armPose.mode() == PFirstPersonRenderMode.VANILLA)
			((ItemInHandRendererAccessor)renderer).pulselib$renderPlayerArm(poseStack, submitNodeCollector, packedLight, inverseArmHeight, attackValue, arm);
	}

	@Redirect(method = "renderTwoHandedMap", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderMapHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/world/entity/HumanoidArm;)V"))
	private void pulselib$renderFirstPersonMapHand(ItemInHandRenderer renderer,
	                                               PoseStack poseStack,
	                                               SubmitNodeCollector submitNodeCollector,
	                                               int packedLight,
	                                               HumanoidArm arm)
	{
		FirstPersonRenderContext context = PULSELIB$FIRST_PERSON_CONTEXT.get();
		PFirstPersonArmPose armPose = context == null ? null : armPose(context.pose(), arm);
		if (armPose == null || armPose.mode() == PFirstPersonRenderMode.VANILLA)
			((ItemInHandRendererAccessor)renderer).pulselib$renderMapHand(poseStack, submitNodeCollector, packedLight, arm);
	}

	@Redirect(method = {"renderOneHandedMap", "renderTwoHandedMap"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/world/item/ItemStack;)V"))
	private void pulselib$renderFirstPersonMap(ItemInHandRenderer renderer,
	                                           PoseStack poseStack,
	                                           SubmitNodeCollector submitNodeCollector,
	                                           int packedLight,
	                                           ItemStack stack)
	{
		FirstPersonRenderContext context = PULSELIB$FIRST_PERSON_CONTEXT.get();
		if (context == null)
		{
			((ItemInHandRendererAccessor)renderer).pulselib$renderMap(poseStack, submitNodeCollector, packedLight, stack);
			return;
		}

		InteractionHand hand = PULSELIB$RENDERED_HAND.get();
		PFirstPersonItemPose itemPose = itemPose(context, itemDisplayContext(context.player(), hand), hand, stack);
		switch (itemPose.mode())
		{
			case VANILLA -> ((ItemInHandRendererAccessor)renderer).pulselib$renderMap(poseStack, submitNodeCollector, packedLight, stack);
			case ANIMATED -> ((ItemInHandRendererAccessor)renderer).pulselib$renderMap(poseStack, submitNodeCollector, packedLight, stack);
			case HIDDEN -> { }
		}
	}

	private static PFirstPersonArmPose armPose(PPlayerFirstPersonPose pose, HumanoidArm arm)
	{
		return arm == HumanoidArm.RIGHT ? pose.rightArm() : pose.leftArm();
	}

	private static PFirstPersonItemPose itemPose(FirstPersonRenderContext context,
	                                             ItemDisplayContext displayContext,
	                                             InteractionHand hand,
	                                             ItemStack stack)
	{
		if (hand != null && context.pose().hidesItem(context.player(), hand, stack))
			return PFirstPersonItemPose.hidden();
		return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? context.pose().rightItem() : context.pose().leftItem();
	}

	private static ItemDisplayContext itemDisplayContext(AbstractClientPlayer player, InteractionHand hand)
	{
		HumanoidArm arm = hand == InteractionHand.OFF_HAND ? player.getMainArm().getOpposite() : player.getMainArm();
		return arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
	}

	private record FirstPersonRenderContext(PPlayerFirstPersonPose pose, LocalPlayer player)
	{
	}
}
