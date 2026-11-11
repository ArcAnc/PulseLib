/**
 * @author ArcAnc
 * Created at: 05.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid.armor;

import com.arcanc.pulselib.util.attachments.PLivingAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Composes existing NeoForge item extensions with PulseLib vanilla-armor replacement rules. */
public class PArmorClientExtensions implements IClientItemExtensions
{
	private static final Model<Unit> EMPTY_ARMOR_MODEL = new Model.Simple(new ModelPart(List.of(), Map.of()), RenderTypes :: armorCutoutNoCull);
	private final IClientItemExtensions base;

	/** Retains the native no-argument entry point with the standard item extension. */
	public PArmorClientExtensions()
	{
		this(IClientItemExtensions.DEFAULT);
	}

	/** Wraps existing item behavior while shared extraction hooks own armor provider selection. */
	public PArmorClientExtensions(IClientItemExtensions base)
	{
		this.base = Objects.requireNonNull(base);
	}

	/** Builds the native default wrapper for an applicable item. */
	public static IClientItemExtensions buildFor(Item item)
	{
		return buildFor(item, IClientItemExtensions.DEFAULT);
	}

	/** Preserves existing extensions for unrelated items and composes applicable armor behavior. */
	public static IClientItemExtensions buildFor(Item item, @Nullable IClientItemExtensions base)
	{
		IClientItemExtensions resolvedBase = base == null ? IClientItemExtensions.DEFAULT : base;
		if (!PLivingAttachments.contains(item) && !(item instanceof PArmorModelProvider))
			return resolvedBase;
		return new PArmorClientExtensions(resolvedBase);
	}

	/** Delegates item font selection to the wrapped extension. */
	@Override
	public @Nullable Font getFont(ItemStack stack, FontContext context)
	{
		return this.base.getFont(stack, context);
	}

	/** Delegates held-item arm pose selection to the wrapped extension. */
	@Override
	public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack)
	{
		return this.base.getArmPose(entity, hand, stack);
	}

	/** Delegates the first-person item transform to the wrapped extension. */
	@Override
	public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
	                                       float partialTick, float equip, float swing)
	{
		return this.base.applyForgeHandTransform(pose, player, arm, stack, partialTick, equip, swing);
	}

	/** Native model queries keep the wrapped extension; shared extraction selects Pulse armor providers. */
	@Override
	public Model<?> getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original)
	{
		return this.base.getHumanoidArmorModel(stack, type, original);
	}

	/** Attachment replacement takes precedence over delegated generic armor geometry. */
	@Override
	public Model<?> getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original)
	{
		if (PLivingAttachments.hidesVanillaArmor(stack))
			return EMPTY_ARMOR_MODEL;
		return this.base.getGenericArmorModel(stack, type, original);
	}

	/** Runs explicit base animation setup and the canonical Pulse provider callback in order. */
	@Override
	public void setupModelAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot, Model model,
	                                 float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch)
	{
		this.base.setupModelAnimations(entity, stack, slot, model, limbSwing, limbAmount, partialTick, age, yaw, pitch);
		if (model instanceof HumanoidModel<?> humanoid && stack.getItem() instanceof PArmorModelProvider provider)
			provider.setupPulseArmorAnimations(entity, stack, slot, humanoid, partialTick);
	}

	/** Delegates the native slot-aware first-person overlay extraction. */
	@Override
	public void renderFirstPersonOverlay(ItemStack stack, EquipmentSlot slot, Player player, GuiGraphicsExtractor graphics, DeltaTracker delta)
	{
		this.base.renderFirstPersonOverlay(stack, slot, player, graphics, delta);
	}

	/** Returns whether the wrapped extension enables dropped-item bobbing. */
	@Override
	public boolean shouldBobAsEntity(ItemStack stack)
	{
		return this.base.shouldBobAsEntity(stack);
	}

	/** Returns whether the wrapped extension enables dropped-item stack spreading. */
	@Override
	public boolean shouldSpreadAsEntity(ItemStack stack)
	{
		return this.base.shouldSpreadAsEntity(stack);
	}

	/** Retains native default tint policy and delegates explicit custom tint behavior. */
	@Override
	public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int fallbackColor)
	{
		if (PLivingAttachments.hidesVanillaArmor(stack))
			return 0;
		return this.base == IClientItemExtensions.DEFAULT ? fallbackColor : this.base.getArmorLayerTintColor(stack, layer, layerIdx, fallbackColor);
	}

	/** Returns the default dye color selected by the wrapped extension. */
	@Override
	public int getDefaultDyeColor(ItemStack stack)
	{
		return this.base.getDefaultDyeColor(stack);
	}

	/** Delegates the native armor texture query, including optional custom layers. */
	@Override
	public @Nullable Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier original)
	{
		return this.base.getArmorTexture(stack, type, layer, original);
	}

	/** Returns the scope overlay selected by the wrapped extension. */
	@Override
	public Identifier getScopeOverlayTexture(ItemStack stack)
	{
		return this.base.getScopeOverlayTexture(stack);
	}

	/** Selects a humanoid model for an explicit canonical entity/slot invocation. */
	public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
	{
		if (PLivingAttachments.hidesVanillaArmor(stack, slot, entity))
		{
			original.root().getAllParts().forEach(part -> part.visible = false);
			return original;
		}
		Model<?> selected = this.base.getHumanoidArmorModel(stack, layerType(entity, slot), original);
		HumanoidModel<?> humanoid = selected instanceof HumanoidModel<?> model ? model : original;
		return stack.getItem() instanceof PArmorModelProvider provider ?
				Objects.requireNonNull(provider.getPulseArmorModel(entity, stack, slot, humanoid), "Pulse armor model") : humanoid;
	}

	/** Returns hidden, provider or delegated geometry for the canonical entity/slot invocation. */
	public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
	{
		if (PLivingAttachments.hidesVanillaArmor(stack, slot, entity))
			return EMPTY_ARMOR_MODEL;
		if (stack.getItem() instanceof PArmorModelProvider)
			return getHumanoidArmorModel(entity, stack, slot, original);
		return this.base.getGenericArmorModel(stack, layerType(entity, slot), original);
	}

	/** Adapts the canonical helmet overlay name to the native slot-aware callback. */
	public void renderHelmetOverlay(ItemStack stack, Player player, GuiGraphicsExtractor graphics, DeltaTracker delta)
	{
		this.base.renderFirstPersonOverlay(stack, EquipmentSlot.HEAD, player, graphics, delta);
	}

	/** Retains canonical entity-aware attachment eligibility for explicit tint queries. */
	public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, EquipmentClientInfo.Layer layer, int layerIdx, int fallbackColor)
	{
		EquipmentSlot slot = entity.getEquipmentSlotForItem(stack);
		if (PLivingAttachments.hidesVanillaArmor(stack, slot, entity))
			return 0;
		return this.base == IClientItemExtensions.DEFAULT ? fallbackColor : this.base.getArmorLayerTintColor(stack, layer, layerIdx, fallbackColor);
	}

	private static EquipmentClientInfo.LayerType layerType(LivingEntity entity, EquipmentSlot slot)
	{
		if (entity.isBaby() && entity.getType() != EntityType.ARMOR_STAND)
			return EquipmentClientInfo.LayerType.HUMANOID_BABY;
		return slot == EquipmentSlot.LEGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
	}
}
