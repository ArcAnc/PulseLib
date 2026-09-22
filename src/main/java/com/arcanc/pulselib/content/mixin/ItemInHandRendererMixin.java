package com.arcanc.pulselib.content.mixin;

import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonArmPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonItemPose;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonPoseStack;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderContext;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderContexts;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonRenderMode;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonTransformMode;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonRenderer;
import com.arcanc.pulselib.content.player.animation.firstPerson.PVanillaFirstPersonArmResolver;
import com.arcanc.pulselib.content.player.animation.firstPerson.PVanillaFirstPersonItemResolver;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Integrates PulseLib's version-independent first-person presentation into the
 * direct 1.21.1 hand renderer. Vanilla still computes equip, swing and use
 * transforms before these final arm and item submissions are replaced.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin
{
	@WrapMethod(method = "renderHandsWithItems")
	private void pulselib$withFirstPersonPass(float partialTick, PoseStack poseStack,
	                                          MultiBufferSource.BufferSource buffers, LocalPlayer player, int packedLight,
	                                          Operation<Void> original)
	{
		PFirstPersonRenderContexts.beginPass(player, partialTick);
		try { original.call(partialTick, poseStack, buffers, player, packedLight); }
		finally { PFirstPersonRenderContexts.endPass(); }
	}

	/** The BufferSource is still open immediately before the vanilla batch flush. */
	@Inject(method = "renderHandsWithItems", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V", shift = At.Shift.BEFORE))
	private void pulselib$renderFirstPersonAttachments(float partialTick, PoseStack poseStack,
	                                                   MultiBufferSource.BufferSource buffers, LocalPlayer player, int packedLight,
	                                                   CallbackInfo ci)
	{
		var presentation = PFirstPersonRenderContexts.passPresentation();
		if (presentation != null) PPlayerFirstPersonRenderer.renderExtras(player, presentation, poseStack, buffers, packedLight, partialTick);
	}

	@WrapMethod(method = "renderArmWithItem")
	private void pulselib$renderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch,
	                                        InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress,
	                                        PoseStack poseStack, MultiBufferSource buffers, int packedLight, Operation<Void> original)
	{
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		PFirstPersonRenderContexts.push(hand, arm, stack, poseStack);
		try
		{
			original.call(player, partialTick, pitch, hand, swingProgress, stack, equippedProgress, poseStack, buffers, packedLight);
			if (hand == InteractionHand.OFF_HAND && stack.isEmpty()) renderHeldArm(player, PFirstPersonRenderContexts.current(), poseStack, buffers, packedLight);
		}
		finally { PFirstPersonRenderContexts.pop(); }
	}

	@WrapOperation(method = "renderPlayerArm", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"))
	private void pulselib$renderRightArm(PlayerRenderer renderer, PoseStack poseStack, MultiBufferSource buffers,
	                                    int packedLight, AbstractClientPlayer player, Operation<Void> original)
	{
		renderArm(renderer, poseStack, buffers, packedLight, player, original);
	}

	@WrapOperation(method = "renderPlayerArm", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"))
	private void pulselib$renderLeftArm(PlayerRenderer renderer, PoseStack poseStack, MultiBufferSource buffers,
	                                   int packedLight, AbstractClientPlayer player, Operation<Void> original)
	{
		renderArm(renderer, poseStack, buffers, packedLight, player, original);
	}

	@WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void pulselib$renderHeldItem(ItemInHandRenderer renderer, LivingEntity entity, ItemStack stack,
	                                    ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
	                                    MultiBufferSource buffers, int packedLight, Operation<Void> original)
	{
		PFirstPersonRenderContext context = PFirstPersonRenderContexts.current();
		if (context == null || context.map())
		{
			original.call(renderer, entity, stack, displayContext, leftHand, poseStack, buffers, packedLight);
			return;
		}
		PFirstPersonItemPose itemPose = context.arm() == HumanoidArm.RIGHT ? context.presentation().rightItem() : context.presentation().leftItem();
		renderHeldArm(entity, context, poseStack, buffers, packedLight);
		if (itemPose.mode() == PFirstPersonRenderMode.HIDDEN) return;
		if (itemPose.mode() == PFirstPersonRenderMode.VANILLA || itemPose.transform() == null)
		{
			original.call(renderer, entity, stack, displayContext, leftHand, poseStack, buffers, packedLight);
			return;
		}
		poseStack.pushPose();
		try
		{
			if (itemPose.transformMode() == PFirstPersonTransformMode.ADDITIVE) poseStack.mulPose(itemPose.transform().matrix());
			else PFirstPersonPoseStack.replaceLocalPose(poseStack, context.basePose(), context.baseNormal(),
					PVanillaFirstPersonItemResolver.resolveItemOrigin(context.arm(), itemPose.transform()));
			original.call(renderer, entity, stack, displayContext, leftHand, poseStack, buffers, packedLight);
		}
		finally { poseStack.popPose(); }
	}

	private static void renderArm(PlayerRenderer renderer, PoseStack poseStack, MultiBufferSource buffers, int packedLight,
	                              AbstractClientPlayer player, Operation<Void> original)
	{
		PFirstPersonRenderContext context = PFirstPersonRenderContexts.current();
		if (context == null || context.map()) { original.call(renderer, poseStack, buffers, packedLight, player); return; }
		PFirstPersonArmPose armPose = context.arm() == HumanoidArm.RIGHT ? context.presentation().rightArm() : context.presentation().leftArm();
		if (armPose.mode() == PFirstPersonRenderMode.HIDDEN) return;
		if (armPose.mode() == PFirstPersonRenderMode.VANILLA || armPose.transform() == null)
		{
			original.call(renderer, poseStack, buffers, packedLight, player);
			return;
		}
		poseStack.pushPose();
		try
		{
			PFirstPersonPoseStack.replaceLocalPose(poseStack, context.basePose(), context.baseNormal(),
					PVanillaFirstPersonArmResolver.resolveArmOrigin(context.arm(), armPose.transform()));
			original.call(renderer, poseStack, buffers, packedLight, player);
		}
		finally { poseStack.popPose(); }
	}

	private static void renderHeldArm(LivingEntity entity, @Nullable PFirstPersonRenderContext context,
	                                  PoseStack poseStack, MultiBufferSource buffers, int packedLight)
	{
		if (context == null || context.map() || !(entity instanceof AbstractClientPlayer player) || player.isInvisible()) return;
		PFirstPersonArmPose armPose = context.arm() == HumanoidArm.RIGHT ? context.presentation().rightArm() : context.presentation().leftArm();
		if (armPose.mode() != PFirstPersonRenderMode.ANIMATED || armPose.transform() == null) return;
		EntityRenderer<?> candidate = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
		if (!(candidate instanceof PlayerRenderer renderer)) return;
		poseStack.pushPose();
		try
		{
			PFirstPersonPoseStack.replaceLocalPose(poseStack, context.basePose(), context.baseNormal(),
					PVanillaFirstPersonArmResolver.resolveArmOrigin(context.arm(), armPose.transform()));
			if (context.arm() == HumanoidArm.RIGHT) renderer.renderRightHand(poseStack, buffers, packedLight, player);
			else renderer.renderLeftHand(poseStack, buffers, packedLight, player);
		}
		finally { poseStack.popPose(); }
	}
}
