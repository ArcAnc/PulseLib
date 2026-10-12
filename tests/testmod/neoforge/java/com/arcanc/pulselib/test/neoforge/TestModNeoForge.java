/**
 * @author ArcAnc
 * Created at: 12.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.neoforge;

import com.arcanc.pulselib.content.registration.block.TestBlock;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.item.TestArmor;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.item.TestTailItem;
import com.arcanc.pulselib.test.DemoContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod ("pulselibtest")
public final class TestModNeoForge
{
	public TestModNeoForge(IEventBus bus)
	{
		var blocks = DeferredRegister.createBlocks("pulselibtest");
		var entities = DeferredRegister.create(Registries.ENTITY_TYPE, "pulselibtest");
		var blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "pulselibtest");
		var items = DeferredRegister.createItems("pulselibtest");
		DemoContent.block = blocks.register("test_block", () -> new TestBlock(BlockBehaviour.Properties.of().
				noOcclusion()));
		DemoContent.blockEntity = blockEntities.register("test_block_entity", () -> BlockEntityType.Builder.of(TestBlockEntity :: new, DemoContent.block.get()).
				build(null));
		DemoContent.entity = entities.register("test_entity", () -> EntityType.Builder.of(TestEntity :: new, MobCategory.MISC).
				sized(1, 1).clientTrackingRange(5).build(DemoContent.id("test_entity").toString()));
		DemoContent.blockItem = items.register("test_block", () -> new TestBlockItem(DemoContent.block.get(), new Item.Properties()));
		DemoContent.helmet = items.register("test_hat", () -> new TestArmor(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(33))));
		DemoContent.chestplate = items.register("test_chest", () -> new TestArmor(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));
		DemoContent.leggings = items.register("test_leggings", () -> new TestArmor(ArmorMaterials.DIAMOND, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(33))));
		DemoContent.tail = items.register("test_tail", () -> new TestTailItem(new Item.Properties().stacksTo(1)));
		blocks.register(bus);
		entities.register(bus);
		blockEntities.register(bus);
		items.register(bus);
		bus.addListener((EntityAttributeCreationEvent event) -> event.put(DemoContent.entity.get(), TestEntity.createAttributes().
				build()));
		if (FMLLoader.getDist().isClient())
			TestModNeoForgeClient.register(bus);
	}
}
