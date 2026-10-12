/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.fabric;

import com.arcanc.pulselib.content.registration.block.TestBlock;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.item.TestArmor;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.item.TestTailItem;
import com.arcanc.pulselib.test.DemoContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class TestModFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		var block = Registry.register(BuiltInRegistries.BLOCK, DemoContent.id("test_block"), new TestBlock(BlockBehaviour.Properties.of().
				noOcclusion()));
		DemoContent.block = () -> block;
		var blockEntity = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DemoContent.id("test_block_entity"), FabricBlockEntityTypeBuilder.create(TestBlockEntity :: new, block).
				build());
		DemoContent.blockEntity = () -> blockEntity;
		var entity = Registry.register(BuiltInRegistries.ENTITY_TYPE, DemoContent.id("test_entity"), FabricEntityTypeBuilder.create(MobCategory.MISC, TestEntity :: new).
				dimensions(EntityDimensions.scalable(1, 1)).trackRangeBlocks(5).build());
		DemoContent.entity = () -> entity;
		FabricDefaultAttributeRegistry.register(entity, TestEntity.createAttributes());
		var blockItem = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_block"), new TestBlockItem(block, new Item.Properties()));
		DemoContent.blockItem = () -> blockItem;
		var helmet = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_hat"), new TestArmor(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(33))));
		DemoContent.helmet = () -> helmet;
		var chestplate = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_chest"), new TestArmor(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));
		DemoContent.chestplate = () -> chestplate;
		var leggings = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_leggings"), new TestArmor(ArmorMaterials.DIAMOND, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(33))));
		DemoContent.leggings = () -> leggings;
		var tail = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_tail"), new TestTailItem(new Item.Properties().stacksTo(1)));
		DemoContent.tail = () -> tail;
	}
}
