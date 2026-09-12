/**
 * @author ArcAnc
 * Created at: 11.09.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.mixin;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor
{
	@Accessor("mainHandItem")
	ItemStack pulselib$mainHandItem();

	@Accessor("offHandItem")
	ItemStack pulselib$offHandItem();

	@Accessor("mainHandHeight")
	float pulselib$mainHandHeight();

	@Accessor("oMainHandHeight")
	float pulselib$oMainHandHeight();

	@Accessor("offHandHeight")
	float pulselib$offHandHeight();

	@Accessor("oOffHandHeight")
	float pulselib$oOffHandHeight();

	@Accessor("itemModelResolver")
	ItemModelResolver pulselib$itemModelResolver();

	@Invoker("renderMap")
	void pulselib$renderMap(PoseStack poseStack,
	                         SubmitNodeCollector submitNodeCollector,
	                         int packedLight,
	                         ItemStack stack);

	@Invoker("renderPlayerArm")
	void pulselib$renderPlayerArm(PoseStack poseStack,
	                              SubmitNodeCollector submitNodeCollector,
	                              int packedLight,
	                              float inverseArmHeight,
	                              float attack,
	                              HumanoidArm arm);

	@Invoker("renderArmWithItem")
	void pulselib$renderArmWithItem(
			AbstractClientPlayer player,
			float partialTick,
			float xRot,
			InteractionHand hand,
			float attack,
			ItemStack stack,
			float inverseArmHeight,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int packedLight);

	@Invoker("applyItemArmTransform")
	void pulselib$applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight);

	@Invoker("applyEatTransform")
	void pulselib$applyEatTransform(PoseStack poseStack,
	                                float partialTick,
	                                HumanoidArm arm,
	                                ItemStack stack,
	                                Player player);

	@Invoker("applyBrushTransform")
	void pulselib$applyBrushTransform(PoseStack poseStack,
	                                  float partialTick,
	                                  HumanoidArm arm,
	                                  Player player);

	@Invoker("swingArm")
	void pulselib$swingArm(float attack, PoseStack poseStack, int invert, HumanoidArm arm);
}
