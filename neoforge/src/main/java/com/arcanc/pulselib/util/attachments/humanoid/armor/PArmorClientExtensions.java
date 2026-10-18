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

import java.util.Objects;

/**
 * Composes existing NeoForge item extensions with PulseLib armor models and vanilla-armor replacement
 * rules.
 */
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
	
	/**
	 * Wraps the supplied item extensions with PulseLib armor rendering behavior.
	 *
	 * @param base the accumulated channel value before the current layer
	 */
	public PArmorClientExtensions(IClientItemExtensions base)
	{
		this.base = base;
	}
	
	/**
	 * Wraps applicable items with PulseLib armor behavior while retaining their existing client extensions.
	 *
	 * @param item the item
	 * @param base the accumulated channel value before the current layer; may be {@code null}
	 * @return the applicable armor wrapper or the unchanged resolved base extension
	 */
	public static IClientItemExtensions buildFor(Item item, @Nullable IClientItemExtensions base)
	{
		IClientItemExtensions resolvedBase = base == null ? IClientItemExtensions.DEFAULT : base;
		if (! PLivingAttachments.contains(item) && !(item instanceof PArmorModelProvider))
			return resolvedBase;
		return new PArmorClientExtensions(resolvedBase);
	}
	
	/**
	 * Delegates item font selection to the wrapped client extension.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @param context the context
	 * @return the delegated font, or {@code null} to retain the default font
	 */
	@Override
	public @Nullable Font getFont(ItemStack stack, FontContext context)
	{
		return this.base.getFont(stack, context);
	}
	
	/**
	 * Delegates held-item arm-pose selection to the wrapped client extension.
	 *
	 * @param entityLiving the living entity wearing or holding the item
	 * @param hand the interaction hand being rendered
	 * @param itemStack the item stack supplying render or animation state
	 * @return the delegated arm pose, or {@code null} to retain default pose selection
	 */
	@Override
	public @Nullable HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack)
	{
		return this.base.getArmPose(entityLiving, hand, itemStack);
	}
	
	/**
	 * Delegates the first-person item transform to the wrapped client extension.
	 *
	 * @param poseStack the current model-to-render pose stack
	 * @param player the player whose animation or presentation is being processed
	 * @param arm the humanoid arm whose geometry is being processed
	 * @param itemInHand the item in hand
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param equipProcess the vanilla hand equip animation progress
	 * @param swingProcess the vanilla hand swing animation progress
	 * @return whether the wrapped extension handled the first-person hand transform
	 */
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
	
	/**
	 * Applies attachment armor-hiding rules and selects a custom PulseLib armor model when provided.
	 *
	 * @param livingEntity the living entity wearing or holding the item
	 * @param itemStack the item stack supplying render or animation state
	 * @param equipmentSlot the armor equipment slot being rendered
	 * @param original the original implementation or unmodified value supplied by the render hook
	 * @return the selected humanoid armor model after attachment replacement rules
	 */
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
				Objects.requireNonNull(provider.getPulseArmorModel(livingEntity, itemStack, equipmentSlot, model), "Pulse armor model") : model;
	}
	
	/**
	 * Returns hidden, custom or delegated armor geometry according to the active attachment rules.
	 *
	 * @param livingEntity the living entity wearing or holding the item
	 * @param itemStack the item stack supplying render or animation state
	 * @param equipmentSlot the armor equipment slot being rendered
	 * @param original the original implementation or unmodified value supplied by the render hook
	 * @return hidden, custom or delegated armor geometry according to the active attachment rules
	 */
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
	
	/**
	 * Runs the base item animation setup followed by any PulseLib armor provider callback.
	 *
	 * @param livingEntity the living entity wearing or holding the item
	 * @param itemStack the item stack supplying render or animation state
	 * @param equipmentSlot the armor equipment slot being rendered
	 * @param model the model
	 * @param limbSwing the entity limb animation position
	 * @param limbSwingAmount the entity limb animation amplitude
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 * @param ageInTicks the entity age used by vanilla render animation
	 * @param netHeadYaw the head yaw relative to the body in degrees
	 * @param headPitch the head pitch in degrees
	 */
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
	
	/**
	 * Delegates helmet overlay rendering to the wrapped client extension.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @param player the player whose animation or presentation is being processed
	 * @param guiGraphics the GUI drawing context
	 * @param deltaTracker the delta tracker
	 */
	@Override
	public void renderHelmetOverlay(ItemStack stack, Player player, GuiGraphics guiGraphics, DeltaTracker deltaTracker)
	{
		this.base.renderHelmetOverlay(stack, player, guiGraphics, deltaTracker);
	}
	
	/**
	 * Returns the custom item renderer selected by the wrapped client extension.
	 *
	 * @return the custom item renderer selected by the wrapped client extension
	 */
	@Override
	public BlockEntityWithoutLevelRenderer getCustomRenderer()
	{
		return this.base.getCustomRenderer();
	}
	
	/**
	 * Returns whether the wrapped extension enables dropped-item bobbing.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @return whether the wrapped extension enables dropped-item bobbing
	 */
	@Override
	public boolean shouldBobAsEntity(ItemStack stack)
	{
		return this.base.shouldBobAsEntity(stack);
	}
	
	/**
	 * Returns whether the wrapped extension enables dropped-item stack spreading.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @return whether the wrapped extension enables dropped-item stack spreading
	 */
	@Override
	public boolean shouldSpreadAsEntity(ItemStack stack)
	{
		return this.base.shouldSpreadAsEntity(stack);
	}
	
	/**
	 * Returns a hidden-layer tint when attachments replace armor, otherwise delegating to the base extension.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @param entity the entity being animated or rendered
	 * @param layer the layer contribution being blended
	 * @param layerIdx the layer idx
	 * @param fallbackColor the fallback color
	 * @return a hidden-layer tint when attachments replace armor, otherwise delegating to the base extension
	 */
	@Override
	public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx, int fallbackColor)
	{
		EquipmentSlot slot = entity.getEquipmentSlotForItem(stack);
		return PLivingAttachments.hidesVanillaArmor(stack, slot, entity) ? 0 : this.base.getArmorLayerTintColor(stack, entity, layer, layerIdx, fallbackColor);
	}
	
	/**
	 * Returns the default dye color selected by the wrapped client extension.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @return the default dye color selected by the wrapped client extension
	 */
	@Override
	public int getDefaultDyeColor(ItemStack stack)
	{
		return this.base.getDefaultDyeColor(stack);
	}
	
	/**
	 * Returns the scope overlay texture selected by the wrapped client extension.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @return the scope overlay texture selected by the wrapped client extension
	 */
	@Override
	public ResourceLocation getScopeOverlayTexture(ItemStack stack)
	{
		return this.base.getScopeOverlayTexture(stack);
	}
}
