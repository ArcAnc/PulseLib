/**
 * @author ArcAnc
 * Created at: 07.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
/**
 * Defines the contract for living attachment source.
 */
public interface PLivingAttachmentSource
{
	/**
	 * Performs the should render operation.
	 * @param entity the entity to use.
	 * @param slot the slot to use.
	 * @param stack the stack to use.
	 * @return the value produced by this operation.
	 */
	boolean shouldRender(LivingEntity entity, EquipmentSlot slot, ItemStack stack);
}
