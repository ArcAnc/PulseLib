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
import com.arcanc.pulselib.test.DemoChecks;
import com.arcanc.pulselib.test.DemoContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class TestModFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		DemoChecks.runDirectory();
		DemoChecks.jar(TestModFabric.class);
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
		{
			if (server.isDedicatedServer())
				DemoChecks.serverStarted(server);
		});
		ServerTickEvents.END_SERVER_TICK.register(server ->
		{
			if (server.isDedicatedServer())
				DemoChecks.serverTick(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server ->
		{
			if (server.isDedicatedServer())
				DemoChecks.serverStopped();
		});
		var block = Registry.register(BuiltInRegistries.BLOCK, DemoContent.id("test_block"), new TestBlock(BlockBehaviour.Properties.of().
				setId(ResourceKey.create(Registries.BLOCK, DemoContent.id("test_block"))).
				noOcclusion()));
		DemoContent.block = () -> block;
		var blockEntity = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DemoContent.id("test_block_entity"), FabricBlockEntityTypeBuilder.create(TestBlockEntity::new, block).
				build());
		DemoContent.blockEntity = () -> blockEntity;
		var entity = Registry.register(BuiltInRegistries.ENTITY_TYPE, DemoContent.id("test_entity"), EntityType.Builder.of(TestEntity::new, MobCategory.MISC).
				sized(1, 1).clientTrackingRange(5).build(ResourceKey.create(Registries.ENTITY_TYPE, DemoContent.id("test_entity"))));
		DemoContent.entity = () -> entity;
		FabricDefaultAttributeRegistry.register(entity, TestEntity.createAttributes());
		var blockItem = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_block"), new TestBlockItem(block, properties("test_block").
				useBlockDescriptionPrefix()));
		DemoContent.blockItem = () -> blockItem;
		var helmet = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_hat"), new TestArmor(properties("test_hat").
				humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)));
		DemoContent.helmet = () -> helmet;
		var chestplate = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_chest"), new TestArmor(properties("test_chest").
				humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.CHESTPLATE)));
		DemoContent.chestplate = () -> chestplate;
		var leggings = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_leggings"), new TestArmor(properties("test_leggings").
				humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.LEGGINGS)));
		DemoContent.leggings = () -> leggings;
		var tail = Registry.register(BuiltInRegistries.ITEM, DemoContent.id("test_tail"), new TestTailItem(properties("test_tail").stacksTo(1)));
		DemoContent.tail = () -> tail;
	}
	private static Item.Properties properties(String name)
	{
		return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, DemoContent.id(name)));
	}
}
