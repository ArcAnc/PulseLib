package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.registration.block.TestBlock;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** The original demo objects; each loader owns their vanilla registry lifecycle. */
public final class DemoContent
{
	public static Supplier<TestBlock> block;
	public static Supplier<BlockEntityType<TestBlockEntity>> blockEntity;
	public static Supplier<EntityType<TestEntity>> entity;
	public static Supplier<? extends Item> blockItem;
	public static Supplier<? extends Item> chestplate;
	public static Supplier<? extends Item> helmet;
	public static Supplier<? extends Item> leggings;
	public static Supplier<? extends Item> tail;

	private DemoContent() { }

	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath("pulselibtest", path);
	}
}
