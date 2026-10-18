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
	/**
	 * Selects the non-null humanoid model used to render the current armor piece.
	 *
	 * @param entity the entity being animated or rendered
	 * @param stack the item stack supplying render or animation state
	 * @param slot the equipment slot being inspected
	 * @param original the original implementation or unmodified value supplied by the render hook
	 * @return the non-null humanoid model to render for this armor piece
	 */
	HumanoidModel<?> getPulseArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original);

	/**
	 * Updates the selected armor model for the current entity and render sample.
	 *
	 * @param entity the entity being animated or rendered
	 * @param stack the item stack supplying render or animation state
	 * @param slot the equipment slot being inspected
	 * @param model the model
	 * @param partialTick the interpolation fraction between the previous and current game ticks
	 */
	default void setupPulseArmorAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
	                                       HumanoidModel<?> model, float partialTick)
	{
	}
}
