/**
 * @author ArcAnc
 * Created at: 13.04.2026
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

public record AnimManagerKey(long key)
{
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
	
	public static AnimManagerKey of(ItemStack stack)
	{
		int itemId = Item.getId(stack.getItem());
		int count = stack.getCount();
		int dataMap = stack.getComponents().hashCode();
		return new AnimManagerKey(Objects.hash(itemId, count, dataMap));
	}
	
	public static AnimManagerKey of (Entity entity)
	{
		return new AnimManagerKey(entity.getUUID().getMostSignificantBits() ^
				entity.getUUID().getLeastSignificantBits());
	}
	
	public static AnimManagerKey of(BlockEntity blockEntity)
	{
		int type = blockEntity.getType().hashCode();
		long blockPos = blockEntity.getBlockPos().asLong();
		int levelId = blockEntity.hasLevel() ? blockEntity.getLevel().dimension().hashCode() : 0;
		
		return new AnimManagerKey(Objects.hash(type, blockPos, levelId));
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (! (o instanceof AnimManagerKey (long other)))
			return false;
		return key() == other;
	}
	
	@Override
	public int hashCode()
	{
		return Long.hashCode(key());
	}
}
