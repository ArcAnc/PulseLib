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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
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
	@WrapMethod(method = "renderHandsWithItems")
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

	@Inject(method = "renderHandsWithItems", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures()V",
			shift = At.Shift.BEFORE))
	private void pulselib$renderExtraFirstPersonGeometry(float partialTick,
	                                                     PoseStack poseStack,
	                                                     SubmitNodeCollector submitNodeCollector,
	                                                     LocalPlayer player,
	                                                     int packedLight,
	                                                     CallbackInfo ci)
	{
		var pose = PFirstPersonRenderContexts.passPose();
		if (pose != null)
			PPlayerFirstPersonRenderer.renderExtras(player, pose, poseStack, submitNodeCollector, packedLight, partialTick);
	}

	@WrapMethod(method = "renderArmWithItem")
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

	@WrapOperation(method = "renderPlayerArm", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;ZLnet/minecraft/client/player/AbstractClientPlayer;)V"))
	private void pulselib$renderRightHand(AvatarRenderer<?> renderer,
	                                     PoseStack poseStack,
	                                     SubmitNodeCollector submitNodeCollector,
	                                     int packedLight,
	                                     Identifier skin,
	                                     boolean sleeve,
	                                     AbstractClientPlayer player,
	                                     Operation<Void> original)
	{
		renderArm(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player, original);
	}

	@WrapOperation(method = "renderPlayerArm", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;ZLnet/minecraft/client/player/AbstractClientPlayer;)V"))
	private void pulselib$renderLeftHand(AvatarRenderer<?> renderer,
	                                    PoseStack poseStack,
	                                    SubmitNodeCollector submitNodeCollector,
	                                    int packedLight,
	                                    Identifier skin,
	                                    boolean sleeve,
	                                    AbstractClientPlayer player,
	                                    Operation<Void> original)
	{
		renderArm(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player, original);
	}

	private static void renderArm(AvatarRenderer<?> renderer,
	                              PoseStack poseStack,
	                              SubmitNodeCollector submitNodeCollector,
	                              int packedLight,
	                              Identifier skin,
	                              boolean sleeve,
	                              AbstractClientPlayer player,
	                              Operation<Void> original)
	{
		PFirstPersonRenderContext context = PFirstPersonRenderContexts.current();
		if (context == null || context.map())
		{
			original.call(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player);
			return;
		}
		PFirstPersonArmPose armPose = context.arm() == HumanoidArm.RIGHT ?
				context.animationPose().rightArm() : context.animationPose().leftArm();
		if (armPose.mode() == PFirstPersonRenderMode.HIDDEN)
			return;
		if (armPose.mode() == PFirstPersonRenderMode.VANILLA)
		{
			original.call(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player);
			return;
		}
		poseStack.pushPose();
		try
		{
			if (armPose.transformMode() == PFirstPersonTransformMode.ADDITIVE)
				poseStack.mulPose(armPose.transform().matrix());
			else
				PFirstPersonPoseStack.replaceLocalPose(poseStack, context.basePose(), context.baseNormal(),
						PVanillaFirstPersonArmResolver.resolveArmOrigin(context.arm(), armPose.transform()));
			original.call(renderer, poseStack, submitNodeCollector, packedLight, skin, sleeve, player);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	@WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target =
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
				context.animationPose().rightItem() : context.animationPose().leftItem();
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
				context.animationPose().rightArm() : context.animationPose().leftArm();
		if (armPose.mode() != PFirstPersonRenderMode.ANIMATED)
			return;
		poseStack.pushPose();
		try
		{
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
