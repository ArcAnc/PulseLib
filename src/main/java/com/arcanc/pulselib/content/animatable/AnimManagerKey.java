/**
 * @author ArcAnc
 * Created at: 06.04.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.animatable;


import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Immutable value object representing anim manager key.
 */
public record AnimManagerKey(long key)
{
	/**
	 * Performs the of object operation.
	 * @param animatable the animatable to use.
	 * @return the value produced by this operation.
	 */
	public static AnimManagerKey ofObject(Object animatable)
	{
		return switch (animatable)
		{
			case ItemStack stack -> of(stack);
			case BlockEntity be -> of(be);
			case Entity entity -> of(entity);
			default -> new AnimManagerKey(animatable.hashCode());
		};
	}
	
	/**
	 * Performs the of operation.
	 * @param stack the stack to use.
	 * @return the value produced by this operation.
	 */
	public static AnimManagerKey of(ItemStack stack)
	{
		int itemId = Item.getId(stack.getItem());
		int count = stack.getCount();
		int dataMap = stack.getComponents().hashCode();
		return new AnimManagerKey(Objects.hash(itemId, count, dataMap));
	}
	
	/**
	 * Performs the of operation.
	 * @param entity the entity to use.
	 * @return the value produced by this operation.
	 */
	public static AnimManagerKey of (Entity entity)
	{
		return new AnimManagerKey(entity.getUUID().hashCode());
	}
	
	/**
	 * Performs the of operation.
	 * @param blockEntity the block entity to use.
	 * @return the value produced by this operation.
	 */
	public static AnimManagerKey of(BlockEntity blockEntity)
	{
		int type = blockEntity.getType().hashCode();
		long blockPos = blockEntity.getBlockPos().asLong();
		int levelId = blockEntity.hasLevel() ? blockEntity.getLevel().dimension().hashCode() : 0;
		
		return new AnimManagerKey(Objects.hash(type, blockPos, levelId));
	}
	
	/**
	 * Performs the equals operation.
	 * @param o the o to use.
	 * @return the value produced by this operation.
	 */
	@Override
	public boolean equals(Object o)
	{
		if (! (o instanceof AnimManagerKey (long other)))
			return false;
		return key() == other;
	}
	
	/**
	 * Performs the hash code operation.
	 * @return the value produced by this operation.
	 */
	@Override
	public int hashCode()
	{
		return Long.hashCode(key());
	}
}
