/**
 * @author ArcAnc
 * Created at: 27.01.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.content.registration;

import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.content.model.animation.PAnimationChannelType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventType;
import com.arcanc.pulselib.content.model.animation.PAnimationEventTypes;
import com.arcanc.pulselib.content.model.deformer.*;
import com.arcanc.pulselib.content.registration.block.TestBlock;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;

public class PLibRegistration
{
	public static class AnimationChannelReg
	{
		public static final PRegistry<PAnimationChannelType<?>> CHANNEL_TYPES = new PRegistry<>();

		public static final PAnimationChannelType<Vector3f> POSITION = CHANNEL_TYPES.register(
				PLibDatabase.rl("position"), new PAnimationChannel.Vector3fChannelType(PLibDatabase.rl("position"), new Vector3f(), false));
		public static final PAnimationChannelType<Quaternionf> ROTATION = CHANNEL_TYPES.register(
				PLibDatabase.rl("rotation"), new PAnimationChannel.QuaternionChannelType(PLibDatabase.rl("rotation")));
		public static final PAnimationChannelType<Vector3f> SCALE = CHANNEL_TYPES.register(
				PLibDatabase.rl("scale"), new PAnimationChannel.Vector3fChannelType(PLibDatabase.rl("scale"), new Vector3f(1f), true));

		private static void init()
		{
		}
	}

	public static class AnimationEventReg
	{
		public static final PRegistry<PAnimationEventType<?>> EVENT_TYPES = new PRegistry<>();

		public static final PAnimationEventType<PAnimationEventTypes.SoundData> SOUND =
				EVENT_TYPES.register(PAnimationEventTypes.SOUND.id(), PAnimationEventTypes.SOUND);
		public static final PAnimationEventType<PAnimationEventTypes.ParticleData> PARTICLE =
				EVENT_TYPES.register(PAnimationEventTypes.PARTICLE.id(), PAnimationEventTypes.PARTICLE);
		public static final PAnimationEventType<PAnimationEventTypes.CameraShakeData> CAMERA_SHAKE =
				EVENT_TYPES.register(PAnimationEventTypes.CAMERA_SHAKE.id(), PAnimationEventTypes.CAMERA_SHAKE);
		public static final PAnimationEventType<PAnimationEventTypes.LocatorCallbackData> LOCATOR_CALLBACK =
				EVENT_TYPES.register(PAnimationEventTypes.LOCATOR_CALLBACK.id(), PAnimationEventTypes.LOCATOR_CALLBACK);
		public static final PAnimationEventType<PAnimationEventTypes.AnimationParameterData> ANIMATION_PARAMETER =
				EVENT_TYPES.register(PAnimationEventTypes.ANIMATION_PARAMETER.id(), PAnimationEventTypes.ANIMATION_PARAMETER);

		private static void init()
		{
		}
	}
	
	public static class MeshDeformerReg
	{
		public static final PRegistry<PMeshDeformer<?>> DEFORMERS = new PRegistry<>();

		public static final PMeshDeformer<PBendDefinition> BEND = DEFORMERS.register(PBendDeformer.INSTANCE.id(), PBendDeformer.INSTANCE);
		public static final PMeshDeformer<PHingeDefinition> HINGE = DEFORMERS.register(PHingeDeformer.INSTANCE.id(), PHingeDeformer.INSTANCE);
		public static final PMeshDeformer<PTwistDefinition> TWIST = DEFORMERS.register(PTwistDeformer.INSTANCE.id(), PTwistDeformer.INSTANCE);
		public static final PMeshDeformer<PStretchDefinition> STRETCH = DEFORMERS.register(PStretchDeformer.INSTANCE.id(), PStretchDeformer.INSTANCE);
		public static final PMeshDeformer<PSquashDefinition> SQUASH = DEFORMERS.register(PSquashDeformer.INSTANCE.id(), PSquashDeformer.INSTANCE);
		public static final PMeshDeformer<PTaperDefinition> TAPER = DEFORMERS.register(PTaperDeformer.INSTANCE.id(), PTaperDeformer.INSTANCE);
		public static final PMeshDeformer<PWaveDefinition> WAVE = DEFORMERS.register(PWaveDeformer.INSTANCE.id(), PWaveDeformer.INSTANCE);

		private static void init()
		{
		}
	}

	public static class BlockReg
	{
		public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PLibDatabase.MOD_ID);
		
		public static final DeferredBlock<TestBlock> TEST_BLOCK = BLOCKS.register("test_block", () -> new TestBlock(
				BlockBehaviour.Properties.of().
						noOcclusion()));
		
		private static void init (@NotNull final IEventBus bus)
		{
			BLOCKS.register(bus);
		}
	}
	
	public static class BETypeReg
	{
		public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
				BuiltInRegistries.BLOCK_ENTITY_TYPE, PLibDatabase.MOD_ID);
	
		public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlockEntity>> TEST_BLOCK_ENTITY = BLOCK_ENTITIES.register("test_block_entity", () ->
				new BlockEntityType<>(TestBlockEntity :: new, Set.of(BlockReg.TEST_BLOCK.get()), null));
		
		private static void init (@NotNull final IEventBus bus)
		{
			BLOCK_ENTITIES.register(bus);
		}
	}
	
	public static class ItemReg
	{
		public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PLibDatabase.MOD_ID);
		
		public static final DeferredItem<TestBlockItem> TEST_ITEM = ITEMS.register("test_block", identifier -> new TestBlockItem(
				BlockReg.TEST_BLOCK.get(),
				new Item.Properties()));
		
		/*public static final DeferredItem<TestArmor> TEST_CHESTPLATE = ITEMS.register("test_chest", identifier -> new TestArmor(
				ArmorMaterials.DIAMOND,
				ArmorItem.Type.CHESTPLATE,
				new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));
		
		public static final DeferredItem<TestArmor> TEST_HAT = ITEMS.register("test_hat", identifier -> new TestArmor(
				ArmorMaterials.DIAMOND,
				ArmorItem.Type.HELMET,
				new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(33))));
		
		public static final DeferredItem<TestArmor> TEST_LEGGINGS = ITEMS.register("test_leggings", identifier -> new TestArmor(
				ArmorMaterials.DIAMOND,
				ArmorItem.Type.LEGGINGS,
				new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(33))));
		
		public static final DeferredItem<TestTailItem> TEST_TAIL = ITEMS.register("test_tail", identifier -> new TestTailItem(
				new Item.Properties().stacksTo(1)));*/
		
		private static void init (@NotNull final IEventBus bus)
		{
			ITEMS.register(bus);
		}
	}
	
	public static class EntityTypeReg
	{
		public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PLibDatabase.MOD_ID);
		
		public static final DeferredHolder<EntityType<?>, EntityType<TestEntity>> TEST_ENTITY = ENTITIES.register("test_entity", key -> EntityType.Builder.of(TestEntity :: new, MobCategory.MISC).
				sized(1, 1).
				clientTrackingRange(5).
				build(key.toString()));
		
		private static void init (@NotNull final IEventBus bus)
		{
			ENTITIES.register(bus);
		}
	}
	
	public static void init(@NotNull final IEventBus bus)
	{
		AnimationChannelReg.init();
		AnimationEventReg.init();
		MeshDeformerReg.init();
		
		/*EntityTypeReg.init(bus);
		BlockReg.init(bus);
		BETypeReg.init(bus);
		ItemReg.init(bus);*/
	}
}
