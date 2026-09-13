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

public class PLivingAttachments
{
	private static final Map<Item, List<PLivingAttachmentDefinition>> ITEM_ATTACHMENTS = new Reference2ObjectLinkedOpenHashMap<>();
	private static final List<PLivingAttachmentDefinition> GLOBAL_ATTACHMENTS = new ArrayList<>();
	
	/**
	 * Performs the register operation.
	 * @param item the item to use.
	 * @param definition the definition to use.
	 */
	public static void register(Item item, PLivingAttachmentDefinition definition)
	{
		ITEM_ATTACHMENTS.computeIfAbsent(item, $ -> new ArrayList<>()).add(definition);
	}
	
	/**
	 * Performs the register operation.
	 * @param item the item to use.
	 * @param definitions the definitions to use.
	 */
	public static void register(Item item, Collection<? extends PLivingAttachmentDefinition> definitions)
	{
		definitions.forEach(definition -> register(item, definition));
	}
	
	/**
	 * Registers the global.
	 * @param definition the definition to use.
	 */
	public static void registerGlobal(PLivingAttachmentDefinition definition)
	{
		GLOBAL_ATTACHMENTS.add(definition);
	}
	
	/**
	 * Performs the contains operation.
	 * @param item the item to use.
	 * @return the value produced by this operation.
	 */
	public static boolean contains(Item item)
	{
		return ITEM_ATTACHMENTS.containsKey(item);
	}
	
	/**
	 * Performs the get operation.
	 * @param stack the stack to use.
	 * @param slot the slot to use.
	 * @param entity the entity to use.
	 * @return the value produced by this operation.
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
	 * Returns the global.
	 * @param entity the entity to use.
	 * @return the value produced by this operation.
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
	 * Performs the hides vanilla armor operation.
	 * @param stack the stack to use.
	 * @param slot the slot to use.
	 * @param entity the entity to use.
	 * @return the value produced by this operation.
	 */
	public static boolean hidesVanillaArmor(ItemStack stack, EquipmentSlot slot, LivingEntity entity)
	{
		for (PLivingAttachmentDefinition definition : get(stack, slot, entity))
			if (definition.hideVanilla())
				return true;
		
		return false;
	}
	
	/**
	 * Performs the hides vanilla armor operation.
	 * @param stack the stack to use.
	 * @return the value produced by this operation.
	 */
	public static boolean hidesVanillaArmor(ItemStack stack)
	{
		List<PLivingAttachmentDefinition> definitions = ITEM_ATTACHMENTS.get(stack.getItem());
		if (definitions == null)
			return false;
		
		for (PLivingAttachmentDefinition definition : definitions)
			if (definition.hideVanilla())
				return true;
		
		return false;
	}
	
	/**
	 * Performs the items operation.
	 * @return the value produced by this operation.
	 */
	public static Collection<Item> items()
	{
		return ITEM_ATTACHMENTS.keySet();
	}
}
