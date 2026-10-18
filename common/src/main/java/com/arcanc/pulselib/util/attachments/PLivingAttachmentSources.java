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

import java.util.function.Predicate;

/**
 * Factory methods for equipment, hand and entity-predicate attachment selectors.
 */
public final class PLivingAttachmentSources
{
	private PLivingAttachmentSources()
	{
	}
	
	/**
	 * Creates a source that accepts supported equipment slots.
	 *
	 * @return the attachment selector with the requested equipment and entity restrictions
	 */
	public static PLivingAttachmentSource anyEquipmentSlot()
	{
		return (entity, slot, stack) -> true;
	}
	
	/**
	 * Creates a source restricted to the supplied equipment slot.
	 *
	 * @param slot the equipment slot being inspected
	 * @return the attachment selector with the requested equipment and entity restrictions
	 */
	public static PLivingAttachmentSource equipmentSlot(EquipmentSlot slot)
	{
		return (entity, currentSlot, stack) -> currentSlot == slot;
	}
	
	/**
	 * Creates a source restricted to held-item equipment slots.
	 *
	 * @return the attachment selector with the requested equipment and entity restrictions
	 */
	public static PLivingAttachmentSource hand()
	{
		return (entity, slot, stack) -> slot.getType() == EquipmentSlot.Type.HAND;
	}
	
	/**
	 * Creates a source selected by a living-entity predicate.
	 *
	 * @param predicate the predicate
	 * @return the attachment selector with the requested equipment and entity restrictions
	 */
	public static PLivingAttachmentSource entityPredicate(Predicate<LivingEntity> predicate)
	{
		return (entity, slot, stack) -> predicate.test(entity);
	}
	
	/**
	 * Creates a source requiring both the selected equipment slot and entity predicate.
	 *
	 * @param slot the equipment slot being inspected
	 * @param predicate the predicate
	 * @return the attachment selector with the requested equipment and entity restrictions
	 */
	public static PLivingAttachmentSource equipmentSlotPredicate(EquipmentSlot slot, Predicate<LivingEntity> predicate)
	{
		return (entity, currentSlot, stack) -> currentSlot == slot && predicate.test(entity);
	}
}
