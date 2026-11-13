/**
 * @author ArcAnc
 * Created at: 13.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting;

import com.arcanc.pulselib.api.plugin.IPulseCommonPlugin;
import com.arcanc.pulselib.api.plugin.PulsePlugin;
import com.arcanc.pulselib.api.registration.PTypeRegistrar;
import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationData;
import com.arcanc.pulselib.content.model.animation.PAnimationChannel;
import com.arcanc.pulselib.content.registration.PLibRegistration;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** A separate server-side fixture applies the real client ItemStack Mixin for native copy/split checks. */
public final class NativeItemStackChecks implements ModInitializer
{
	private static int registrations;

	@Override
	public void onInitialize()
	{
		check(FabricLoader.getInstance().isModLoaded("pulselib_stage2_fixture"), "Real mod list");
		check(!PulseServices.PLATFORM.isClient(), "Real Fabric physical-side service");
		// Exercise registration with the actual loader/mod list before any world is created.
		// The production SERVER_STARTING callback is not dispatched by this fixture.
		PulsePluginManager.get().registerCommon();
		check(registrations == 1, "Actual finder and manager registration");
		check(PulsePluginManager.get().pluginIds().contains(id("plugin")), "Plugin discovered from fixture mod root");
		check(PLibRegistration.AnimationChannelReg.CHANNEL_TYPES.get(id("channel")).isPresent(), "Registered channel");
		System.out.println("PASS Fabric loader/mod-list discovery, actual PulseLibFabric initialization, platform service and PulsePluginManager");
		var key = ResourceKey.create(Registries.ITEM, id("animated"));
		Item animated = Registry.register(BuiltInRegistries.ITEM, key, new AnimatedItem(new Item.Properties().setId(key)));
		// 26.2 binds prototype components during resource loading, later than onInitialize.
		// Bind only our fixture items explicitly so no datapack/world startup is needed.
		var components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		animated.builtInRegistryHolder().bindComponents(components);
		var plainKey = ResourceKey.create(Registries.ITEM, id("plain"));
		Item plainItem = Registry.register(BuiltInRegistries.ITEM, plainKey, new Item(new Item.Properties().setId(plainKey)));
		plainItem.builtInRegistryHolder().bindComponents(components);
		ItemStack source = new ItemStack(animated, 8);
		long original = data(source).pulselib$getOrCreateAnimationId();
		check(original > 0, "Native animated identity");
		check(data(source.copy()).pulselib$getAnimationId() == original, "Native copy preserves identity");
		ItemStack part = source.split(3);
		check(source.getCount() == 5 && part.getCount() == 3, "Native partial split counts");
		check(data(source).pulselib$getAnimationId() == original && data(part).pulselib$getAnimationId() > 0 &&
				data(part).pulselib$getAnimationId() != original, "Native partial split separates identities");
		ItemStack rest = source.split(5);
		check(source.isEmpty() && data(source).pulselib$getAnimationId() == 0 &&
				data(rest).pulselib$getAnimationId() == original, "Native full split transfers identity");
		ItemStack plain = new ItemStack(plainItem, 2);
		check(data(plain).pulselib$getOrCreateAnimationId() == 0 && data(plain.copy()).pulselib$getAnimationId() == 0,
				"Native plain item stays unassigned");
		check(data(ItemStack.EMPTY).pulselib$getOrCreateAnimationId() == 0, "EMPTY identity");
		data(ItemStack.EMPTY).pulselib$setAnimationId(original);
		check(data(ItemStack.EMPTY).pulselib$getAnimationId() == 0, "EMPTY cannot acquire identity");
		System.out.println("PASS real ItemStackMixin transformation: copy, partial/full split, plain and EMPTY; no controller access");
		System.out.println("PULSELIB_NATIVE_FIXTURE_PASSED; stopping before dedicated server/world startup.");
		System.exit(0);
	}

	private static PItemStackAnimationData data(ItemStack stack)
	{
		return (PItemStackAnimationData)(Object)stack;
	}

	private static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath("porting_fixture", path);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	@PulsePlugin(PulsePlugin.Side.SERVER)
	public static final class FixturePlugin implements IPulseCommonPlugin
	{
		@Override
		public Identifier getPluginId()
		{
			return id("plugin");
		}

		@Override
		public void registerAnimationChannels(PTypeRegistrar registrar)
		{
			registrations++;
			registrar.registerAnimationChannel(new PAnimationChannel.Vector3fChannelType(id("channel"), new Vector3f(), false));
		}
	}

	private static final class AnimatedItem extends Item implements PAnimatable<AnimatedItem>
	{
		private AnimatedItem(Properties properties)
		{
			super(properties);
		}

		@Override
		public PAnimationManager<AnimatedItem> getAnimationManager(AnimManagerKey key)
		{
			throw new AssertionError("Identity operations must not request animation managers");
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<AnimatedItem> registrar)
		{
			throw new AssertionError("Identity operations must not create animation controllers");
		}
	}
}
