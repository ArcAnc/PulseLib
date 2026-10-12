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

public final class PLivingAttachmentSources
{
	private PLivingAttachmentSources()
	{
	}
	
	public static PLivingAttachmentSource anyEquipmentSlot()
	{
		return (entity, slot, stack) -> true;
	}
	
	public static PLivingAttachmentSource equipmentSlot(EquipmentSlot slot)
	{
		return (entity, currentSlot, stack) -> currentSlot == slot;
	}
	
	public static PLivingAttachmentSource hand()
	{
		return (entity, slot, stack) -> slot.getType() == EquipmentSlot.Type.HAND;
	}
	
	public static PLivingAttachmentSource entityPredicate(Predicate<LivingEntity> predicate)
	{
		return (entity, slot, stack) -> predicate.test(entity);
	}
	
	public static PLivingAttachmentSource equipmentSlotPredicate(EquipmentSlot slot, Predicate<LivingEntity> predicate)
	{
		return (entity, currentSlot, stack) -> currentSlot == slot && predicate.test(entity);
	}
}
