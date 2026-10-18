/**
 * @author ArcAnc
 * Created at: 04.07.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.util.attachments;


import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Registry of item-specific and global living attachment definitions.
 */
public class PLivingAttachments
{
	private static final Map<Item, List<PLivingAttachmentDefinition>> ITEM_ATTACHMENTS = new Reference2ObjectLinkedOpenHashMap<>();
	private static final List<PLivingAttachmentDefinition> GLOBAL_ATTACHMENTS = new ArrayList<>();
	
	/**
	 * Associates attachment definitions with the supplied item.
	 *
	 * @param item the item
	 * @param definition the immutable configuration to register or evaluate
	 */
	public static void register(Item item, PLivingAttachmentDefinition definition)
	{
		List<PLivingAttachmentDefinition> definitions = ITEM_ATTACHMENTS.computeIfAbsent(item, $ -> new ArrayList<>());
		if (!definitions.contains(definition))
			definitions.add(definition);
	}
	
	/**
	 * Associates attachment definitions with the supplied item.
	 *
	 * @param item the item
	 * @param definitions the definitions
	 */
	public static void register(Item item, Collection<? extends PLivingAttachmentDefinition> definitions)
	{
		definitions.forEach(definition -> register(item, definition));
	}
	
	/**
	 * Registers an attachment considered for all eligible living entities.
	 *
	 * @param definition the immutable configuration to register or evaluate
	 */
	public static void registerGlobal(PLivingAttachmentDefinition definition)
	{
		if (!GLOBAL_ATTACHMENTS.contains(definition))
			GLOBAL_ATTACHMENTS.add(definition);
	}
	
	/**
	 * Returns whether the supplied item has registered attachment definitions.
	 *
	 * @param item the item
	 * @return whether the supplied item has registered attachment definitions
	 */
	public static boolean contains(Item item)
	{
		return ITEM_ATTACHMENTS.containsKey(item);
	}
	
	/**
	 * Returns item attachments that match the supplied stack, slot and entity.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @param slot the equipment slot being inspected
	 * @param entity the entity being animated or rendered
	 * @return item attachments that match the supplied stack, slot and entity
	 */
	public static List<PLivingAttachmentDefinition> get(ItemStack stack, EquipmentSlot slot, LivingEntity entity)
	{
		List<PLivingAttachmentDefinition> definitions = ITEM_ATTACHMENTS.get(stack.getItem());
		if (definitions == null)
			return List.of();
		
		List<PLivingAttachmentDefinition> result = new ArrayList<>();
		for (PLivingAttachmentDefinition definition : definitions)
			if (definition.source().shouldRender(entity, slot, stack))
				result.add(definition);
		
		return result;
	}
	
	/**
	 * Returns global attachment definitions that match the supplied entity.
	 *
	 * @param entity the entity being animated or rendered
	 * @return global attachment definitions that match the supplied entity
	 */
	public static List<PLivingAttachmentDefinition> getGlobal(LivingEntity entity)
	{
		List<PLivingAttachmentDefinition> result = new ArrayList<>();
		for (PLivingAttachmentDefinition definition : GLOBAL_ATTACHMENTS)
			if (definition.source().shouldRender(entity, EquipmentSlot.BODY, ItemStack.EMPTY))
				result.add(definition);
		
		return result;
	}
	
	/**
	 * Returns whether an eligible attachment replaces the supplied vanilla armor piece.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @param slot the equipment slot being inspected
	 * @param entity the entity being animated or rendered
	 * @return whether an eligible attachment replaces the supplied vanilla armor piece
	 */
	public static boolean hidesVanillaArmor(ItemStack stack, EquipmentSlot slot, LivingEntity entity)
	{
		for (PLivingAttachmentDefinition definition : get(stack, slot, entity))
			if (definition.hideVanilla())
				return true;
		
		return false;
	}
	
	/**
	 * Returns the items that have registered attachment definitions.
	 *
	 * @return the items that have registered attachment definitions
	 */
	public static Collection<Item> items()
	{
		return ITEM_ATTACHMENTS.keySet();
	}
}
