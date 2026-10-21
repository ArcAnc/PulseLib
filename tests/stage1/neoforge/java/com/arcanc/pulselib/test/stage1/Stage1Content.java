/**
 * @author ArcAnc
 * Created at: 21.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.registration.block.TestBlock;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import com.arcanc.pulselib.util.PLibDatabase;

/** Original 26.1 demo registrations, confined to the stage 1 verification fixture. */
public final class Stage1Content
{
/**
 * Provides support for block reg.
 */
	public static class BlockReg
	{
		public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PLibDatabase.MOD_ID);
		
		public static final DeferredBlock<TestBlock> TEST_BLOCK = BLOCKS.register("test_block", () -> new TestBlock(
				BlockBehaviour.Properties.of().setId(
						ResourceKey.create(Registries.BLOCK, PLibDatabase.rl("test_block"))).
						noOcclusion()));
		
		/**
		 * Performs the init operation.
		 * @param bus the bus to use.
		 */
		private static void init (@NotNull final IEventBus bus)
		{
			BLOCKS.register(bus);
		}
	}
	
/**
 * Provides support for be type reg.
 */
	public static class BETypeReg
	{
		public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
				BuiltInRegistries.BLOCK_ENTITY_TYPE, PLibDatabase.MOD_ID);
	
		public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlockEntity>> TEST_BLOCK_ENTITY = BLOCK_ENTITIES.register("test_block_entity", () ->
				new BlockEntityType<>(TestBlockEntity :: new, BlockReg.TEST_BLOCK.get()));
		
		/**
		 * Performs the init operation.
		 * @param bus the bus to use.
		 */
		private static void init (@NotNull final IEventBus bus)
		{
			BLOCK_ENTITIES.register(bus);
		}
	}
	
/**
 * Provides support for item reg.
 */
	public static class ItemReg
	{
		public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PLibDatabase.MOD_ID);
		
		public static final DeferredItem<TestBlockItem> TEST_ITEM = ITEMS.register("test_block", identifier -> new TestBlockItem(
				BlockReg.TEST_BLOCK.get(),
				new Item.Properties().setId(
						ResourceKey.create(Registries.ITEM, PLibDatabase.rl("test_block")))));
		
		/*public static final DeferredItem<TestArmorItem> TEST_HAT = ITEMS.registerItem("test_hat",
				TestArmorItem :: new,
				() -> new Item.Properties().humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET));
		
		public static final DeferredItem<TestArmorItem> TEST_CHESTPLATE = ITEMS.registerItem("test_chestplate",
				TestArmorItem :: new,
				() -> new Item.Properties().humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.CHESTPLATE));
		
		public static final DeferredItem<TestArmorItem> TEST_LEGGINGS = ITEMS.registerItem("test_leggings",
				TestArmorItem :: new,
				() -> new Item.Properties().humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.LEGGINGS));
		*/
		/**
		 * Performs the init operation.
		 * @param bus the bus to use.
		 */
		private static void init (@NotNull final IEventBus bus)
		{
			ITEMS.register(bus);
		}
	}
	
/**
 * Provides support for entity type reg.
 */
	public static class EntityTypeReg
	{
		public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(PLibDatabase.MOD_ID);
		
		public static final DeferredHolder<EntityType<?>, EntityType<TestEntity>> TEST_ENTITY = ENTITIES.registerEntityType("test_entity", TestEntity :: new, MobCategory.MISC,
				builder ->
				builder.
						noLootTable().
						sized(1, 1).
						clientTrackingRange(5));
		
		/**
		 * Performs the init operation.
		 * @param bus the bus to use.
		 */
		private static void init (@NotNull final IEventBus bus)
		{
			ENTITIES.register(bus);
		}
	}

	public static void register(IEventBus bus)
	{
		EntityTypeReg.init(bus);
		BlockReg.init(bus);
		BETypeReg.init(bus);
		ItemReg.init(bus);
	}
}
