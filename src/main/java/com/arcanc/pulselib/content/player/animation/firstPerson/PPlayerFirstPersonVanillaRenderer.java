/**
 * @author ArcAnc
 * Created at: 11.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.player.animation.firstPerson;

import com.arcanc.pulselib.content.mixin.ItemInHandRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Replays the per-hand portion of Minecraft's {@link ItemInHandRenderer}.
 *
 * <p>When another first-person channel or attachment requires PulseLib to
 * replace the complete vanilla call, a {@link PFirstPersonRenderMode#VANILLA}
 * channel cannot return to Minecraft's outer renderer. This class keeps that
 * channel on the same transforms, visible-item swap state, and client item
 * extensions as vanilla.
 */
final class PPlayerFirstPersonVanillaRenderer
{
	private PPlayerFirstPersonVanillaRenderer()
	{
	}

	static void renderVanillaArm(
			LocalPlayer player,
			InteractionHand hand,
			HumanoidArm arm,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight,
			float partialTick)
	{
		if (player.isInvisible() || player.isScoping() || !shouldRenderHand(player, hand))
			return;

		VanillaHandState state = state(player, hand, partialTick);
		ItemInHandRendererAccessor renderer = renderer();
		poseStack.pushPose();
		try
		{
			applyViewBobbing(player, poseStack, partialTick);
			renderer.pulselib$renderPlayerArm(
					poseStack, submitNodeCollector, packedLight, state.inverseArmHeight(), state.attack(), arm);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	static void renderVanillaItem(
			LocalPlayer player,
			InteractionHand hand,
			HumanoidArm arm,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight,
			float partialTick)
	{
		if (player.isScoping() || !shouldRenderHand(player, hand))
			return;

		VanillaHandState state = state(player, hand, partialTick);
		if (state.stack().isEmpty())
			return;

		poseStack.pushPose();
		try
		{
			applyViewBobbing(player, poseStack, partialTick);
			if (state.stack().getItem() instanceof MapItem)
				renderMapItem(player, hand, arm, state, poseStack, submitNodeCollector, packedLight);
			else if (state.stack().getItem() instanceof CrossbowItem)
				renderCrossbow(player, hand, arm, state, poseStack, submitNodeCollector, packedLight, partialTick);
			else
				renderItem(player, hand, arm, state, poseStack, submitNodeCollector, packedLight, partialTick);
		}
		finally
		{
			poseStack.popPose();
		}
	}

	private static void renderMapItem(
			LocalPlayer player,
			InteractionHand hand,
			HumanoidArm arm,
			VanillaHandState state,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight)
	{
		if (hand == InteractionHand.MAIN_HAND && renderer().pulselib$offHandItem().isEmpty())
		{
			float sqrtAttack = Mth.sqrt(state.attack());
			float ySwing = -0.2F * Mth.sin(state.attack() * (float)Math.PI);
			float zSwing = -0.4F * Mth.sin(sqrtAttack * (float)Math.PI);
			poseStack.translate(0.0F, -ySwing / 2.0F, zSwing);
			float mapTilt = calculateMapTilt(player.getXRot());
			poseStack.translate(0.0F, 0.04F + state.inverseArmHeight() * -1.2F + mapTilt * -0.5F, -0.72F);
			poseStack.mulPose(Axis.XP.rotationDegrees(mapTilt * -85.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(sqrtAttack * (float)Math.PI) * 20.0F));
			poseStack.scale(2.0F, 2.0F, 2.0F);
			renderer().pulselib$renderMap(poseStack, submitNodeCollector, packedLight, state.stack());
			return;
		}

		float invert = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		poseStack.translate(invert * 0.125F, -0.125F, 0.0F);
		poseStack.translate(invert * 0.51F, -0.08F + state.inverseArmHeight() * -1.2F, -0.75F);
		float sqrtAttack = Mth.sqrt(state.attack());
		float xSwing = Mth.sin(sqrtAttack * (float)Math.PI);
		float xSwingPosition = -0.5F * xSwing;
		float ySwingPosition = 0.4F * Mth.sin(sqrtAttack * (float)(Math.PI * 2));
		float zSwingPosition = -0.3F * Mth.sin(state.attack() * (float)Math.PI);
		poseStack.translate(invert * xSwingPosition, ySwingPosition - 0.3F * xSwing, zSwingPosition);
		poseStack.mulPose(Axis.XP.rotationDegrees(xSwing * -45.0F));
		poseStack.mulPose(Axis.YP.rotationDegrees(invert * xSwing * -30.0F));
		renderer().pulselib$renderMap(poseStack, submitNodeCollector, packedLight, state.stack());
	}

	private static void renderCrossbow(
			LocalPlayer player,
			InteractionHand hand,
			HumanoidArm arm,
			VanillaHandState state,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight,
			float partialTick)
	{
		ItemInHandRendererAccessor renderer = renderer();
		int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
		renderer.pulselib$applyItemArmTransform(poseStack, arm, state.inverseArmHeight());
		boolean charged = CrossbowItem.isCharged(state.stack());
		if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand && !charged)
		{
			poseStack.translate(invert * -0.4785682F, -0.094387F, 0.05731531F);
			poseStack.mulPose(Axis.XP.rotationDegrees(-11.935F));
			poseStack.mulPose(Axis.YP.rotationDegrees(invert * 65.3F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -9.785F));
			float timeHeld = state.stack().getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
			float power = Math.min(timeHeld / CrossbowItem.getChargeDuration(state.stack(), player), 1.0F);
			applyChargeJiggle(poseStack, timeHeld, power);
			poseStack.translate(0.0F, 0.0F, power * 0.04F);
			poseStack.scale(1.0F, 1.0F, 1.0F + power * 0.2F);
			poseStack.mulPose(Axis.YN.rotationDegrees(invert * 45.0F));
		}
		else
		{
			renderer.pulselib$swingArm(state.attack(), poseStack, invert, arm);
			if (charged && state.attack() < 0.001F && hand == InteractionHand.MAIN_HAND)
			{
				poseStack.translate(invert * -0.641864F, 0.0F, 0.0F);
				poseStack.mulPose(Axis.YP.rotationDegrees(invert * 10.0F));
			}
		}
		renderItemModel(player, arm, state.stack(), poseStack, submitNodeCollector, packedLight);
	}

	private static void renderItem(
			LocalPlayer player,
			InteractionHand hand,
			HumanoidArm arm,
			VanillaHandState state,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight,
			float partialTick)
	{
		ItemInHandRendererAccessor renderer = renderer();
		ItemStack stack = state.stack();
		int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
		if (!IClientItemExtensions.of(stack).applyForgeHandTransform(
				poseStack, Minecraft.getInstance().player, arm, stack, partialTick, state.inverseArmHeight(), state.attack()))
		{
			if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand)
			{
				ItemUseAnimation useAnimation = stack.getUseAnimation();
				if (!useAnimation.hasCustomArmTransform())
					renderer.pulselib$applyItemArmTransform(poseStack, arm, state.inverseArmHeight());

				switch (useAnimation)
				{
					case EAT, DRINK ->
					{
						renderer.pulselib$applyEatTransform(poseStack, partialTick, arm, stack, player);
						renderer.pulselib$applyItemArmTransform(poseStack, arm, state.inverseArmHeight());
					}
					case BLOCK ->
					{
						if (!(stack.getItem() instanceof ShieldItem))
						{
							poseStack.translate(invert * -0.14142136F, 0.08F, 0.14142136F);
							poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
							poseStack.mulPose(Axis.YP.rotationDegrees(invert * 13.365F));
							poseStack.mulPose(Axis.ZP.rotationDegrees(invert * 78.05F));
						}
					}
					case BOW -> applyBowTransform(player, stack, poseStack, invert, partialTick);
					case TRIDENT -> applyTridentTransform(player, stack, poseStack, invert, partialTick);
					case BRUSH -> renderer.pulselib$applyBrushTransform(poseStack, partialTick, arm, player);
					case BUNDLE -> renderer.pulselib$swingArm(state.attack(), poseStack, invert, arm);
					case SPEAR ->
					{
						poseStack.translate(invert * 0.56F, -0.52F, -0.72F);
						float timeHeld = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
						SpearAnimations.firstPersonUse(player.getTicksSinceLastKineticHitFeedback(partialTick), poseStack, timeHeld, arm, stack);
					}
					case NONE ->
					{
					}
				}
			}
			else if (player.isAutoSpinAttack())
			{
				renderer.pulselib$applyItemArmTransform(poseStack, arm, state.inverseArmHeight());
				poseStack.translate(invert * -0.4F, 0.8F, 0.3F);
				poseStack.mulPose(Axis.YP.rotationDegrees(invert * 65.0F));
				poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -85.0F));
			}
			else
			{
				renderer.pulselib$applyItemArmTransform(poseStack, arm, state.inverseArmHeight());
				switch (stack.getSwingAnimation().type())
				{
					case WHACK -> renderer.pulselib$swingArm(state.attack(), poseStack, invert, arm);
					case STAB -> SpearAnimations.firstPersonAttack(state.attack(), poseStack, invert, arm);
					case NONE ->
					{
					}
				}
			}
		}
		renderItemModel(player, arm, stack, poseStack, submitNodeCollector, packedLight);
	}

	private static void applyBowTransform(LocalPlayer player, ItemStack stack, PoseStack poseStack, int invert, float partialTick)
	{
		poseStack.translate(invert * -0.2785682F, 0.18344387F, 0.15731531F);
		poseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));
		poseStack.mulPose(Axis.YP.rotationDegrees(invert * 35.3F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -9.785F));
		float timeHeld = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
		float power = timeHeld / 20.0F;
		power = Math.min((power * power + power * 2.0F) / 3.0F, 1.0F);
		applyChargeJiggle(poseStack, timeHeld, power);
		poseStack.translate(0.0F, 0.0F, power * 0.04F);
		poseStack.scale(1.0F, 1.0F, 1.0F + power * 0.2F);
		poseStack.mulPose(Axis.YN.rotationDegrees(invert * 45.0F));
	}

	private static void applyTridentTransform(LocalPlayer player, ItemStack stack, PoseStack poseStack, int invert, float partialTick)
	{
		poseStack.translate(invert * -0.5F, 0.7F, 0.1F);
		poseStack.mulPose(Axis.XP.rotationDegrees(-55.0F));
		poseStack.mulPose(Axis.YP.rotationDegrees(invert * 35.3F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(invert * -9.785F));
		float timeHeld = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
		float power = Math.min(timeHeld / 10.0F, 1.0F);
		applyChargeJiggle(poseStack, timeHeld, power);
		poseStack.translate(0.0F, 0.0F, power * 0.2F);
		poseStack.scale(1.0F, 1.0F, 1.0F + power * 0.2F);
		poseStack.mulPose(Axis.YN.rotationDegrees(invert * 45.0F));
	}

	private static void applyChargeJiggle(PoseStack poseStack, float timeHeld, float power)
	{
		if (power > 0.1F)
		{
			float shake = Mth.sin((timeHeld - 0.1F) * 1.3F) * (power - 0.1F);
			poseStack.translate(0.0F, shake * 0.004F, 0.0F);
		}
	}

	private static void renderItemModel(
			LocalPlayer player,
			HumanoidArm arm,
			ItemStack stack,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight)
	{
		Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderItem(
				player,
				stack,
				arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
				poseStack,
				submitNodeCollector,
				packedLight);
	}

	private static VanillaHandState state(LocalPlayer player, InteractionHand hand, float partialTick)
	{
		ItemInHandRendererAccessor renderer = renderer();
		boolean mainHand = hand == InteractionHand.MAIN_HAND;
		ItemStack stack = mainHand ? renderer.pulselib$mainHandItem() : renderer.pulselib$offHandItem();
		float height = mainHand ?
				1.0F - Mth.lerp(partialTick, renderer.pulselib$oMainHandHeight(), renderer.pulselib$mainHandHeight()) :
				1.0F - Mth.lerp(partialTick, renderer.pulselib$oOffHandHeight(), renderer.pulselib$offHandHeight());
		float inverseArmHeight = renderer.pulselib$itemModelResolver().swapAnimationScale(stack) * height;
		InteractionHand attackHand = player.swingingArm == null ? InteractionHand.MAIN_HAND : player.swingingArm;
		float attack = attackHand == hand ? player.getAttackAnim(partialTick) : 0.0F;
		return new VanillaHandState(stack, inverseArmHeight, attack);
	}

	private static boolean shouldRenderHand(LocalPlayer player, InteractionHand hand)
	{
		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();
		boolean holdsBow = mainHand.is(Items.BOW) || offHand.is(Items.BOW);
		boolean holdsCrossbow = mainHand.is(Items.CROSSBOW) || offHand.is(Items.CROSSBOW);
		if (!holdsBow && !holdsCrossbow)
			return true;
		if (player.isUsingItem())
		{
			ItemStack usedItem = player.getUseItem();
			InteractionHand usedHand = player.getUsedItemHand();
			if (usedItem.is(Items.BOW) || usedItem.is(Items.CROSSBOW))
				return hand == usedHand;
			return hand == InteractionHand.MAIN_HAND || !(usedHand == InteractionHand.MAIN_HAND && CrossbowItem.isCharged(offHand));
		}
		return hand == InteractionHand.MAIN_HAND || !CrossbowItem.isCharged(mainHand);
	}

	private static void applyViewBobbing(LocalPlayer player, PoseStack poseStack, float partialTick)
	{
		float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
		float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
		poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1F));
		poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1F));
	}

	private static float calculateMapTilt(float xRot)
	{
		float tilt = Mth.clamp(1.0F - xRot / 45.0F + 0.1F, 0.0F, 1.0F);
		return -Mth.cos(tilt * (float)Math.PI) * 0.5F + 0.5F;
	}

	private static ItemInHandRendererAccessor renderer()
	{
		return (ItemInHandRendererAccessor)Minecraft.getInstance().gameRenderer.itemInHandRenderer;
	}

	private record VanillaHandState(ItemStack stack, float inverseArmHeight, float attack)
	{
	}
}
