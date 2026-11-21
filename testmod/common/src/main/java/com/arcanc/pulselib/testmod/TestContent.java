/**
 * @author ArcAnc
 * Created at: 21.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;

public final class TestContent
{
	public static final String MOD_ID = "pulselib_test";
	public static final Identifier CREATURE_ID = id("test_entity");
	public static final Identifier BLOCK_ID = id("test_block");

	private static @Nullable Block block;
	private static @Nullable BlockEntityType<TestBlockEntity> blockEntity;
	private static @Nullable EntityType<TestCreature> creature;

	private TestContent()
	{
	}

	public static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void register(ResourceKey<? extends Registry<?>> registry)
	{
		if (registry.equals(Registries.BLOCK))
		{
			var key = ResourceKey.create(Registries.BLOCK, BLOCK_ID);
			block = Registry.register(BuiltInRegistries.BLOCK, key, new AnimatedBlock(Block.Properties.of().setId(key).noOcclusion()));
		}
		else if (registry.equals(Registries.ENTITY_TYPE))
		{
			var key = ResourceKey.create(Registries.ENTITY_TYPE, CREATURE_ID);
			creature = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
					EntityType.Builder.of(TestCreature::new, MobCategory.MISC).sized(1, 1).clientTrackingRange(8).build(key));
		}
		else if (registry.equals(Registries.ITEM))
		{
			var key = ResourceKey.create(Registries.ITEM, BLOCK_ID);
			Registry.register(BuiltInRegistries.ITEM, key, new TestItem(Objects.requireNonNull(block), new Item.Properties().setId(key)));
		}
	}

	public static void registerBlockEntity(Function<Block, BlockEntityType<TestBlockEntity>> factory)
	{
		blockEntity = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BLOCK_ID, factory.apply(Objects.requireNonNull(block)));
	}

	public static BlockEntityType<TestBlockEntity> blockEntity()
	{
		return Objects.requireNonNull(blockEntity);
	}

	public static EntityType<TestCreature> creature()
	{
		return Objects.requireNonNull(creature);
	}

	private static final class AnimatedBlock extends Block implements EntityBlock
	{
		private AnimatedBlock(Properties properties)
		{
			super(properties);
		}

		@Override
		public TestBlockEntity newBlockEntity(BlockPos pos, BlockState state)
		{
			return new TestBlockEntity(pos, state);
		}

		@Override
		protected RenderShape getRenderShape(BlockState state)
		{
			return RenderShape.INVISIBLE;
		}
	}
}
