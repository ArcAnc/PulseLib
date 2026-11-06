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
import com.arcanc.pulselib.test.DemoChecks;
import com.arcanc.pulselib.test.DemoContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod ("pulselibtest")

public final class TestModNeoForge
{
	public TestModNeoForge(IEventBus bus)
	{
		DemoChecks.runDirectory();
		DemoChecks.jar(TestModNeoForge.class);
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
		{
			if (event.getServer().isDedicatedServer())
				DemoChecks.serverStarted(event.getServer());
		});
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) ->
		{
			if (event.getServer().isDedicatedServer())
				DemoChecks.serverTick(event.getServer());
		});
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
		{
			if (event.getServer().isDedicatedServer())
				DemoChecks.serverStopped();
		});
		var blocks = DeferredRegister.createBlocks("pulselibtest");
		var entities = DeferredRegister.create(Registries.ENTITY_TYPE, "pulselibtest");
		var blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "pulselibtest");
		var items = DeferredRegister.createItems("pulselibtest");
		DemoContent.block = blocks.register("test_block", () -> new TestBlock(BlockBehaviour.Properties.of().
				setId(ResourceKey.create(Registries.BLOCK, DemoContent.id("test_block"))).
				noOcclusion()));
		DemoContent.blockEntity = blockEntities.register("test_block_entity", () -> new BlockEntityType<>(TestBlockEntity :: new, DemoContent.block.get()));
		DemoContent.entity = entities.register("test_entity", () -> EntityType.Builder.of(TestEntity :: new, MobCategory.MISC).
				sized(1, 1).clientTrackingRange(5).build(ResourceKey.create(Registries.ENTITY_TYPE, DemoContent.id("test_entity"))));
		DemoContent.blockItem = items.register("test_block", () -> new TestBlockItem(DemoContent.block.get(), properties("test_block").useBlockDescriptionPrefix()));
		DemoContent.helmet = items.register("test_hat", () -> new TestArmor(properties("test_hat").humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET)));
		DemoContent.chestplate = items.register("test_chest", () -> new TestArmor(properties("test_chest").
				humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.CHESTPLATE)));
		DemoContent.leggings = items.register("test_leggings", () -> new TestArmor(properties("test_leggings").
				humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.LEGGINGS)));
		DemoContent.tail = items.register("test_tail", () -> new TestTailItem(properties("test_tail").stacksTo(1)));
		blocks.register(bus);
		entities.register(bus);
		blockEntities.register(bus);
		items.register(bus);
		bus.addListener((EntityAttributeCreationEvent event) -> event.put(DemoContent.entity.get(), TestEntity.createAttributes().
				build()));
		if (FMLEnvironment.getDist().isClient())
			TestModNeoForgeClient.register(bus);
	}
	private static Item.Properties properties(String name)
	{
		return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, DemoContent.id(name)));
	}
}
