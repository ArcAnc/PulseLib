/**
 * @author ArcAnc
 * Created at: 02.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62armor;

import com.arcanc.pulselib.util.attachments.humanoid.armor.PArmorModelProvider;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.List;
import java.util.Locale;

/** Shared registry content; client method bodies are never invoked on the dedicated server. */
public final class ArmorContent
{
	public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD);
	public static final List<ProviderItem> ITEMS = SLOTS.stream().map(ProviderItem::new).toList();

	private ArmorContent()
	{
	}

	public static Identifier id(EquipmentSlot slot)
	{
		return Identifier.fromNamespaceAndPath("pulselib_stage62armor", "provider_" + slot.name().toLowerCase(Locale.ROOT));
	}

	public static final class ProviderItem extends Item implements PArmorModelProvider
	{
		private ProviderItem(EquipmentSlot slot)
		{
			super(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(slot))).durability(100).
					component(DataComponents.EQUIPPABLE, Equippable.builder(slot).setAsset(EquipmentAssets.DIAMOND).build()));
		}

		@Override
		public HumanoidModel<?> getPulseArmorModel(LivingEntity owner, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original)
		{
			return ArmorProbe.select(owner, stack, slot, original);
		}

		@Override
		public void setupPulseArmorAnimations(LivingEntity owner, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> model, float partialTick)
		{
			ArmorProbe.animate(owner, stack, slot, model, partialTick);
		}
	}
}
