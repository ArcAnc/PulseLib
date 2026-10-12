/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.registration.block.TestBlock;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The original demo objects; each loader owns their vanilla registry lifecycle.
 */
public final class DemoContent
{
	public static @Nullable Supplier<TestBlock> block;
	public static @Nullable Supplier<BlockEntityType<TestBlockEntity>> blockEntity;
	public static @Nullable Supplier<EntityType<TestEntity>> entity;
	public static @Nullable Supplier<? extends Item> blockItem;
	public static @Nullable Supplier<? extends Item> chestplate;
	public static @Nullable Supplier<? extends Item> helmet;
	public static @Nullable Supplier<? extends Item> leggings;
	public static @Nullable Supplier<? extends Item> tail;

	private DemoContent()
	{
	}

	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath("pulselibtest", path);
	}
}
