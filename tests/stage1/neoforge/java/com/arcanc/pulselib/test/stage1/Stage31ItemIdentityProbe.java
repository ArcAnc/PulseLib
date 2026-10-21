/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.util.helpers.PLibHelper;
import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationData;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.util.PLibDatabase;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/** Direct regressions from the canonical harness, adapted to the opt-in 26.1 fixture. */
public final class Stage31ItemIdentityProbe
{
	private static @Nullable ItemStack survivingStack;
	private static long survivingId;

	private Stage31ItemIdentityProbe()
	{
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
		{
			throw new AssertionError(message);
		}
	}

	private static ItemStack animated(int count)
	{
		return new ItemStack(Stage1Content.ItemReg.TEST_ITEM.get(), count);
	}

	private static PItemStackAnimationData data(ItemStack stack)
	{
		return (PItemStackAnimationData)(Object)stack;
	}

	private static long id(ItemStack stack)
	{
		return data(stack).pulselib$getOrCreateAnimationId();
	}

	private static Map<?, ?> managers()
	{
		return managers(SingletonAnimationManager.class);
	}

	private static Map<?, ?> managers(Class<?> owner)
	{
		try
		{
			var field = owner.getDeclaredField("MANAGERS");
			field.setAccessible(true);
			return (Map<?, ?>)field.get(null);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	private static void pass(int number, String scenario)
	{
		PLibDatabase.LOGGER.info("ITEM IDENTITY REGRESSION {} PASS: {}", number, scenario);
	}

	public static void verify()
	{
		ItemStack first = animated(8);
		ItemStack second = animated(8);
		int managerCount = managers().size();
		check(data(first).pulselib$getAnimationId() == 0, "Construction must leave identity lazy");
		long firstId = id(first);
		check(firstId != 0 && firstId != id(second), "Independent identical stacks need distinct IDs");
		pass(1, "independent identical stacks");
		check(id(first) == firstId && id(first) == firstId, "Repeated reads must retain identity");
		pass(2, "repeated reads");
		ItemStack lazy = animated(8);
		ItemStack copy = lazy.copy();
		long copyId = id(copy);
		check(copyId == id(lazy), "Copy before first render must assign and inherit identity");
		check(id(first.copy()) == firstId, "Copy after assignment must inherit identity");
		pass(3, "ordinary copy before/after first identity access");
		for (int index = 0; index < 100; index++)
		{
			copy = copy.copy();
			check(id(copy) == copyId, "Copy chain must retain identity");
		}
		pass(4, "100-copy chain");
		check(id(lazy.copyWithCount(3)) == copyId, "copyWithCount must inherit identity");
		pass(5, "copyWithCount");
		ItemStack partial = animated(8);
		ItemStack part = partial.split(3);
		check(partial.getCount() == 5 && part.getCount() == 3 && id(partial) != id(part), "Partial split must give the part a new ID");
		long partialId = id(partial);
		check(id(partial.split(1)) != partialId && id(partial) == partialId, "Further partial split must preserve source identity");
		pass(6, "partial split, including an unassigned source");
		long transferred = id(partial);
		ItemStack whole = partial.split(100);
		check(id(whole) == transferred && partial.isEmpty() && data(partial).pulselib$getAnimationId() == 0,
				"Full split must transfer and clear source identity");
		partial.setCount(1);
		check(id(partial) != transferred, "Refilled source after full split needs a new identity");
		ItemStack lazyFull = animated(2);
		ItemStack lazyWhole = lazyFull.split(2);
		check(id(lazyWhole) != 0 && data(lazyFull).pulselib$getAnimationId() == 0, "Lazy full split must transfer and clear identity");
		pass(7, "full split and source refill");
		first.setCount(5);
		first.grow(2);
		first.shrink(1);
		check(id(first) == firstId, "Count changes must retain identity");
		pass(8, "count mutation");
		first.set(DataComponents.CUSTOM_NAME, Component.literal("Runtime identity test"));
		check(id(first) == firstId, "Component mutation must retain identity");
		first.remove(DataComponents.CUSTOM_NAME);
		check(id(first) == firstId, "Component removal must retain identity");
		pass(9, "component mutation/removal");
		ItemStack tool = animated(1);
		tool.set(DataComponents.MAX_STACK_SIZE, 1);
		tool.set(DataComponents.MAX_DAMAGE, 100);
		long toolId = id(tool);
		tool.setDamageValue(17);
		check(tool.getDamageValue() == 17 && id(tool) == toolId, "Damage mutation must retain identity");
		pass(10, "tool durability mutation");
		check(id(ItemStack.EMPTY) == 0 && ItemStack.EMPTY.copy() == ItemStack.EMPTY && ItemStack.EMPTY.split(1) == ItemStack.EMPTY,
				"EMPTY must remain unassigned through copies/splits");
		data(ItemStack.EMPTY).pulselib$setAnimationId(123);
		check(data(ItemStack.EMPTY).pulselib$getAnimationId() == 0, "EMPTY must reject assigned IDs");
		pass(11, "EMPTY");
		ItemStack ordinary = new ItemStack(Items.STONE, 8);
		check(id(ordinary) == 0 && id(ordinary.copy()) == 0 && id(ordinary.split(2)) == 0,
				"Ordinary items should not receive IDs");
		check(managers().size() == managerCount, "ID assignment and copying must not create managers/controllers");
		pass(12, "ordinary items, no managers from identity assignment");
		long started = System.nanoTime();
		for (int index = 0; index < 100_000; index++)
		{
			check(id(lazy.copy()) == copyId, "Mass copies must inherit identity");
		}
		long elapsed = System.nanoTime() - started;
		check(elapsed < TimeUnit.SECONDS.toNanos(10) && managers().size() == managerCount, "100,000 copies must finish without creating managers");
		PLibDatabase.LOGGER.info("ITEM IDENTITY COPY BENCHMARK: 100000 vanilla copies, {} ms, manager delta 0", elapsed / 1_000_000.0);
		pass(13, "100000 copies");
		concurrentAccess();
		check(managers().size() == managerCount, "Concurrent IDs/copies must not register controllers");
		pass(14, "eight concurrent readers/copy callers");
		serialization(first);
		pass(15, "NBT and network round trips");
		SimpleContainer container = new SimpleContainer(1);
		ItemStack destination = animated(5);
		ItemStack incoming = animated(3);
		long destinationId = id(destination);
		check(destinationId != id(incoming) && ItemStack.isSameItemSameComponents(destination, incoming), "Runtime IDs must not affect component equality");
		container.setItem(0, destination);
		check(container.addItem(incoming).isEmpty() && container.getItem(0).getCount() == 8 &&
				id(container.getItem(0)) == destinationId, "Vanilla merging must retain destination identity and counts");
		pass(16, "vanilla component equality and merging");
		check(id(lazy.copy()) == id(lazy.copy()), "Known compromise: independent copies share source identity");
		check(lazy.split(0).isEmpty() && id(lazy) == copyId, "Zero split must preserve source identity");
		verifyManagers(first, second);
		try
		{
			var runtimeField = ItemStack.class.getDeclaredField("pulselib$animationId");
			check(Modifier.isTransient(runtimeField.getModifiers()) && Modifier.isVolatile(runtimeField.getModifiers()), "Injected ID must be transient/volatile");
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError("Runtime identity field is missing", exception);
		}
		survivingStack = first;
		survivingId = firstId;
		PLibDatabase.LOGGER.info("ITEM IDENTITY REGRESSIONS PASS: 16 cases, copy-sharing limitation, independent controllers and phases");
	}

	/** Runs after the library's normal shutdown hook, without invoking cleanup from the probe. */
	public static void shutdown(GameShuttingDownEvent event)
	{
		check(survivingStack != null, "Identity regressions must have completed before shutdown");
		check(managers().isEmpty() && managers(InstanceAnimationManager.class).isEmpty(),
				"Normal shutdown must release both manager registries");
		check(id(survivingStack) == survivingId, "Cleanup must not recycle a surviving stack identity");
		PLibDatabase.LOGGER.info("STAGE 3.1 MANAGER SHUTDOWN PASS: singleton=0, instance=0, surviving ID retained");
	}

	private static void concurrentAccess()
	{
		ItemStack stack = animated(1);
		CountDownLatch start = new CountDownLatch(1);
		var executor = Executors.newFixedThreadPool(8);
		try
		{
			List<CompletableFuture<Long>> tasks = new ArrayList<>();
			for (int index = 0; index < 8; index++)
			{
				tasks.add(CompletableFuture.supplyAsync(() ->
				{
					try
					{
						check(start.await(5, TimeUnit.SECONDS), "Concurrent reader start timed out");
					}
					catch (InterruptedException exception)
					{
						Thread.currentThread().interrupt();
						throw new AssertionError(exception);
					}
					long expected = id(stack.copy());
					for (int read = 0; read < 1000; read++)
					{
						check(id(stack) == expected && id(stack.copy()) == expected, "Concurrent reads/copies assigned different source IDs");
					}
					return expected;
				}, executor));
			}
			start.countDown();
			for (var task : tasks)
			{
				check(task.get(10, TimeUnit.SECONDS) == id(stack), "All readers must see one ID");
			}
		}
		catch (Exception exception)
		{
			throw new AssertionError("Concurrent identity regression failed", exception);
		}
		finally
		{
			executor.shutdownNow();
		}
	}

	private static void serialization(ItemStack source)
	{
		var registries = Minecraft.getInstance().level.registryAccess();
		Tag saved = ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), source).getOrThrow();
		ItemStack loaded = ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), saved).getOrThrow();
		check(data(loaded).pulselib$getAnimationId() == 0 && id(loaded) != id(source), "NBT must not serialize runtime identity");
		check(ItemStack.matches(source, loaded), "NBT item data must remain unchanged");
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		try
		{
			ItemStack.STREAM_CODEC.encode(buffer, source);
			ItemStack received = ItemStack.STREAM_CODEC.decode(buffer);
			check(data(received).pulselib$getAnimationId() == 0 && id(received) != id(source), "Network encoding must not transmit runtime identity");
			check(ItemStack.matches(source, received), "Network item data must remain unchanged");
		}
		finally
		{
			buffer.release();
		}
	}

	private static void verifyManagers(ItemStack first, ItemStack second)
	{
		TestBlockItem item = (TestBlockItem)first.getItem();
		PBakedModel model = TestBlockItemRenderer.MODEL_DATA.getModel();
		var firstManager = PLibHelper.getManager(item, AnimManagerKey.of(first));
		var secondManager = PLibHelper.getManager(item, AnimManagerKey.of(second));
		check(firstManager != secondManager, "Independent stacks must have independent singleton managers");
		firstManager.bindModel(model);
		secondManager.bindModel(model);
		var firstController = firstManager.getControllers().get("default");
		var secondController = secondManager.getControllers().get("default");
		check(firstController != secondController, "Independent stacks must have independent controllers");
		firstController.tick(item, 1, model);
		secondController.tick(item, 1, model);
		float length = model.animations().get("idle").length();
		check(length > 0, "Original item idle clip must have a positive duration");
		firstController.seek(model, length * 0.2f);
		secondController.seek(model, length * 0.7f);
		check(firstController.getTime() != secondController.getTime(), "Independent animation phases must be possible");
		float before = firstController.getTime();
		var copyManager = PLibHelper.getManager(first.copy());
		check(copyManager == firstManager && copyManager.getControllers().get("default").getTime() == before,
				"Temporary copy must reuse the existing manager and phase");
		check(AnimManagerKey.of(first).key() < 0 && AnimManagerKey.of(Minecraft.getInstance().player).key() >= 0 &&
				AnimManagerKey.ofObject(item).key() >= 0, "Item keys must occupy a separate sign-bit domain");
		PLibDatabase.LOGGER.info("ITEM IDENTITY PHASE PROBE: IDs {} / {}, controller times {} / {}, copy reuses manager",
				id(first), id(second), firstController.getTime(), secondController.getTime());
	}
}
