/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments.humanoid.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Implement on an armor item to provide a model on both loaders. Called only on the client.
 */
public interface PArmorModelProvider
{
	HumanoidModel<?> getPulseArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original);

	default void setupPulseArmorAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
	                                       HumanoidModel<?> model, float partialTick)
	{
	}
}
