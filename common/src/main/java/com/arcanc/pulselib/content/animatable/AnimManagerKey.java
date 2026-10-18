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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Identity used to select animation state for an entity, block entity, item stack or arbitrary owner.
 *
 * @param key the key
 */
public record AnimManagerKey(long key)
{
	/**
	 * Builds an animation manager key from the identity of an arbitrary owner.
	 *
	 * @param animatable the owner whose animation state is being processed
	 * @return the identity key for the supplied owner
	 */
	public static AnimManagerKey ofObject(Object animatable)
	{
		return switch (animatable)
		{
			case ItemStack stack -> of(stack);
			case BlockEntity be -> of(be);
			case Entity entity -> of(entity);
			default -> new AnimManagerKey(Integer.toUnsignedLong(animatable.hashCode()));
		};
	}
	
	/**
	 * Gets a client runtime identity. The sign bit reserves item keys without changing
	 * the public long-key API; other factory methods always return nonnegative keys.
	 * @throws IllegalArgumentException if the stack is empty or is not animated.
	 *
	 * @param stack the item stack supplying render or animation state
	 * @return the identity key for the supplied owner
	 */
	public static AnimManagerKey of(ItemStack stack)
	{
		long id = ((PItemStackAnimationData)(Object)stack).pulselib$getOrCreateAnimationId();
		if (stack.isEmpty() || id == 0)
		{
			throw new IllegalArgumentException("Animation keys require a nonempty PItemAnimatable stack");
		}
		return new AnimManagerKey(Long.MIN_VALUE | id);
	}
	
	/**
	 * Builds an animation manager key for the supplied owner.
	 *
	 * @param entity the entity being animated or rendered
	 * @return the identity key for the supplied owner
	 */
	public static AnimManagerKey of (Entity entity)
	{
		return new AnimManagerKey((entity.getUUID().getMostSignificantBits() ^
				entity.getUUID().getLeastSignificantBits()) & Long.MAX_VALUE);
	}
	
	/**
	 * Builds an animation manager key for the supplied owner.
	 *
	 * @param blockEntity the block entity
	 * @return the identity key for the supplied owner
	 */
	public static AnimManagerKey of(BlockEntity blockEntity)
	{
		int type = blockEntity.getType().hashCode();
		long blockPos = blockEntity.getBlockPos().asLong();
		int levelId = blockEntity.hasLevel() ? blockEntity.getLevel().dimension().hashCode() : 0;
		
		return new AnimManagerKey(Integer.toUnsignedLong(Objects.hash(type, blockPos, levelId)));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o)
	{
		if (! (o instanceof AnimManagerKey (long other)))
			return false;
		return key() == other;
	}
	
	/**
	 * Returns the hash code.
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode()
	{
		return Long.hashCode(key());
	}
}
