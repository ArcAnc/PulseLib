/**
 * @author ArcAnc
 * Created at: 02.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid.armor;


import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public class PArmorClientExtensions implements IClientItemExtensions
{
	private static final Model EMPTY_ARMOR_MODEL = new Model(RenderType :: entityCutoutNoCull)
	{
		@Override
		public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
		{
		}
	};
	
	private final IClientItemExtensions base;
	
	public PArmorClientExtensions(IClientItemExtensions base)
	{
		this.base = base;
	}
	
	public static IClientItemExtensions buildFor(Item item, @Nullable IClientItemExtensions base)
	{
		IClientItemExtensions resolvedBase = base == null ? IClientItemExtensions.DEFAULT : base;
		if (! PLivingAttachments.contains(item) && !(item instanceof PArmorModelProvider))
			return resolvedBase;
		return new PArmorClientExtensions(resolvedBase);
	}
	
	@Override
	public @Nullable Font getFont(ItemStack stack, FontContext context)
	{
		return this.base.getFont(stack, context);
	}
	
	@Override
	public @Nullable HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack)
	{
		return this.base.getArmPose(entityLiving, hand, itemStack);
	}
	
	@Override
	public boolean applyForgeHandTransform(PoseStack poseStack,
	                                       LocalPlayer player,
	                                       HumanoidArm arm,
	                                       ItemStack itemInHand,
	                                       float partialTick,
	                                       float equipProcess,
	                                       float swingProcess)
	{
		return this.base.applyForgeHandTransform(poseStack, player, arm, itemInHand, partialTick, equipProcess, swingProcess);
	}
	
	@Override
	public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity,
	                                              ItemStack itemStack,
	                                              EquipmentSlot equipmentSlot,
	                                              HumanoidModel<?> original)
	{
		if (PLivingAttachments.hidesVanillaArmor(itemStack, equipmentSlot, livingEntity))
		{
			original.setAllVisible(false);
			return original;
		}
		
		HumanoidModel<?> model = this.base.getHumanoidArmorModel(livingEntity, itemStack, equipmentSlot, original);
		return itemStack.getItem() instanceof PArmorModelProvider provider ?
				java.util.Objects.requireNonNull(provider.getPulseArmorModel(livingEntity, itemStack, equipmentSlot, model), "Pulse armor model") : model;
	}
	
	@Override
	public Model getGenericArmorModel(LivingEntity livingEntity,
	                                  ItemStack itemStack,
	                                  EquipmentSlot equipmentSlot,
	                                  HumanoidModel<?> original)
	{
		if (PLivingAttachments.hidesVanillaArmor(itemStack, equipmentSlot, livingEntity))
			return EMPTY_ARMOR_MODEL;
		
		if (itemStack.getItem() instanceof PArmorModelProvider)
			return getHumanoidArmorModel(livingEntity, itemStack, equipmentSlot, original);
		return this.base.getGenericArmorModel(livingEntity, itemStack, equipmentSlot, original);
	}
	
	@Override
	public void setupModelAnimations(LivingEntity livingEntity,
	                                 ItemStack itemStack,
	                                 EquipmentSlot equipmentSlot,
	                                 Model model,
	                                 float limbSwing,
	                                 float limbSwingAmount,
	                                 float partialTick,
	                                 float ageInTicks,
	                                 float netHeadYaw,
	                                 float headPitch)
	{
		this.base.setupModelAnimations(livingEntity, itemStack, equipmentSlot, model, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
		if (model instanceof HumanoidModel<?> humanoid && itemStack.getItem() instanceof PArmorModelProvider provider)
			provider.setupPulseArmorAnimations(livingEntity, itemStack, equipmentSlot, humanoid, partialTick);
	}
	
	@Override
	public void renderHelmetOverlay(ItemStack stack, Player player, GuiGraphics guiGraphics, DeltaTracker deltaTracker)
	{
		this.base.renderHelmetOverlay(stack, player, guiGraphics, deltaTracker);
	}
	
	@Override
	public BlockEntityWithoutLevelRenderer getCustomRenderer()
	{
		return this.base.getCustomRenderer();
	}
	
	@Override
	public boolean shouldBobAsEntity(ItemStack stack)
	{
		return this.base.shouldBobAsEntity(stack);
	}
	
	@Override
	public boolean shouldSpreadAsEntity(ItemStack stack)
	{
		return this.base.shouldSpreadAsEntity(stack);
	}
	
	@Override
	public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx, int fallbackColor)
	{
		EquipmentSlot slot = entity.getEquipmentSlotForItem(stack);
		return PLivingAttachments.hidesVanillaArmor(stack, slot, entity) ? 0 : this.base.getArmorLayerTintColor(stack, entity, layer, layerIdx, fallbackColor);
	}
	
	@Override
	public int getDefaultDyeColor(ItemStack stack)
	{
		return this.base.getDefaultDyeColor(stack);
	}
	
	@Override
	public ResourceLocation getScopeOverlayTexture(ItemStack stack)
	{
		return this.base.getScopeOverlayTexture(stack);
	}
}
