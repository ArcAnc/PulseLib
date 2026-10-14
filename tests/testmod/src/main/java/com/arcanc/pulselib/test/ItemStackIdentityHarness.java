/**
 * @author ArcAnc
 * Created at: 14.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationData;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.registration.item.TestBlockItem;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.util.PLibDatabase;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.arcanc.pulselib.test.SmokeHarness.check;
import static com.arcanc.pulselib.test.SmokeHarness.field;

/** Exercises real transformed ItemStacks and vanilla menus in a marked disposable world. */
public final class ItemStackIdentityHarness
{
	private static final BlockPos CHEST = new BlockPos(10, 140, 12);
	private static int phase;
	private static boolean frameActive;
	private static int disconnectHooks;
	private static int startTick;
	private static long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(3);
	private static @Nullable CompletableFuture<?> serverAction;
	private static @Nullable ItemStack survivor;
	private static long survivorId;
	private static long inventoryId;
	private static @Nullable WeakReference<?> managerReference;
	private static @Nullable WeakReference<?> controllerReference;

	private ItemStackIdentityHarness()
	{
	}

	public static void frame()
	{
		// Disconnect/openWorld can render a loading screen before returning to this frame.
		if (frameActive || phase == 12)
		{
			return;
		}
		frameActive = true;
		try
		{
			frameOnce();
		}
		finally
		{
			frameActive = false;
		}
	}

	private static void frameOnce()
	{
		Minecraft mc = Minecraft.getInstance();
		check(System.nanoTime() < deadline, "Item identity game phase timed out: " + phase);
		if (phase == 13)
		{
			if (disconnectHooks == 0)
			{
				return;
			}
			check(managers().isEmpty(), "Final disconnect must clear managers");
			PLibDatabase.LOGGER.info("ITEM IDENTITY CLIENT PASS");
			mc.stop();
			advance(12);
			return;
		}
		if (phase == 10)
		{
			if (disconnectHooks == 0)
			{
				return;
			}
			check(managers().isEmpty(), "Natural disconnect must clear item managers");
			check(id(survivor) == survivorId, "Cleanup must not recycle a surviving stack ID");
			System.gc();
			if (managerReference.get() != null || controllerReference.get() != null)
			{
				return;
			}
			DisposableTestWorlds.reopenCurrentTestWorld();
			advance(11);
			return;
		}
		if (mc.level == null || mc.player == null || mc.getOverlay() != null ||
				TestBlockItemRenderer.MODEL_DATA.getModel() == null)
		{
			return;
		}
		DisposableTestWorlds.requireCurrentWorld();
		if (serverAction != null)
		{
			if (!serverAction.isDone())
			{
				return;
			}
			serverAction.join();
			serverAction = null;
		}
		switch (phase)
		{
			case 0 ->
			{
				regressions();
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				serverAction = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					var level = player.serverLevel();
					for (int x = 8; x <= 12; x++)
					{
						for (int z = 8; z <= 13; z++)
						{
							level.setBlockAndUpdate(new BlockPos(x, 139, z), Blocks.STONE.defaultBlockState());
							for (int y = 140; y <= 143; y++)
							{
								level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
							}
						}
					}
					level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
					player.teleportTo(level, 10.5, 140, 10.5, 0, 0);
					// InventoryScreen otherwise redirects to the creative catalog with different slots.
					player.setGameMode(GameType.SURVIVAL);
					player.getInventory().clearContent();
					player.getInventory().setItem(0, animated(8));
					player.getInventory().setItem(1, animated(8));
					player.getInventory().setItem(40, animated(2));
					player.inventoryMenu.broadcastChanges();
				}, server);
				advance(1);
			}
			case 1 ->
			{
				if (mc.player.getInventory().getItem(0).getCount() != 8 ||
						mc.player.getInventory().getItem(1).getCount() != 8 || mc.player.getOffhandItem().getCount() != 2 ||
						mc.gameMode.hasInfiniteItems())
				{
					return;
				}
				ItemStack first = mc.player.getInventory().getItem(0);
				ItemStack second = mc.player.getInventory().getItem(1);
				check(id(first) != id(second) && id(first) != id(mc.player.getOffhandItem()), "Independent inventory/hand stacks need distinct IDs");
				verifyManagers(first, second);
				inventoryId = id(first);
				click(36, 0, ClickType.PICKUP);
				click(38, 0, ClickType.PICKUP);
				check(id(mc.player.getInventory().getItem(2)) == inventoryId, "Hotbar transfer must retain client identity");
				mc.player.getInventory().selected = 2;
				mc.setScreen(new InventoryScreen(mc.player));
				startTick = mc.player.tickCount;
				advance(2);
			}
			case 2 ->
			{
				if (mc.player.tickCount - startTick < 8)
				{
					return;
				}
				mc.setScreen(null);
				mc.setScreen(new InventoryScreen(mc.player));
				check(id(mc.player.getInventory().getItem(2)) == inventoryId, "Reopening inventory must retain identity");
				check(TestBlockItemRenderer.renders > 0, "Animated items must reach the loader renderer");
				mc.setScreen(null);
				// Real vanilla client prediction, followed by the ordinary server click packet.
				PLibDatabase.LOGGER.info("ITEM IDENTITY MENU BEFORE SPLIT: expected {}, source {} count {}, carried {} count {}",
						inventoryId, id(mc.player.getInventory().getItem(2)), mc.player.getInventory().getItem(2).getCount(),
						id(mc.player.containerMenu.getCarried()), mc.player.containerMenu.getCarried().getCount());
				click(38, 1, ClickType.PICKUP);
				ItemStack remainder = mc.player.getInventory().getItem(2);
				ItemStack detached = mc.player.containerMenu.getCarried();
				PLibDatabase.LOGGER.info("ITEM IDENTITY MENU AFTER SPLIT: source {} count {}, carried {} count {}",
						id(remainder), remainder.getCount(), id(detached), detached.getCount());
				check(remainder.getCount() == 4 && detached.getCount() == 4 &&
						id(remainder) == inventoryId && id(detached) != inventoryId, "Menu partial split must create a new identity");
				long detachedId = id(detached);
				click(39, 0, ClickType.PICKUP);
				check(id(mc.player.getInventory().getItem(3)) == detachedId, "Full carried-stack split must transfer identity");
				click(39, 0, ClickType.PICKUP);
				click(38, 0, ClickType.PICKUP);
				check(mc.player.getInventory().getItem(2).getCount() == 8 &&
						id(mc.player.getInventory().getItem(2)) == inventoryId, "Merge must retain destination identity");
				click(38, 40, ClickType.SWAP);
				check(id(mc.player.getOffhandItem()) == inventoryId, "Swap to offhand must retain client identity");
				click(38, 40, ClickType.SWAP);
				startTick = mc.player.tickCount;
				advance(3);
			}
			case 3 ->
			{
				if (mc.player.tickCount - startTick < 8)
				{
					return;
				}
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				serverAction = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					player.openMenu((ChestBlockEntity)player.serverLevel().getBlockEntity(CHEST));
				}, server);
				advance(4);
			}
			case 4 ->
			{
				if (!(mc.player.containerMenu instanceof ChestMenu menu))
				{
					return;
				}
				ItemStack source = menu.getSlot(56).getItem();
				check(source.getCount() == 8, "Chest must show the authoritative inventory stack");
				long before = id(source);
				check(AnimManagerKey.of(mc.level.getBlockEntity(CHEST)).key() >= 0, "BlockEntity keys must not overlap item keys");
				click(56, 0, ClickType.QUICK_MOVE);
				check(menu.getSlot(0).getItem().getCount() == 8 && id(menu.getSlot(0).getItem()) == before,
						"Inventory-to-chest quick move must inherit identity");
				click(0, 0, ClickType.PICKUP);
				click(56, 0, ClickType.PICKUP);
				check(id(menu.getSlot(56).getItem()) == before, "Chest-to-inventory transfer must inherit identity");
				mc.player.closeContainer();
				startTick = mc.player.tickCount;
				advance(5);
			}
			case 5 ->
			{
				if (mc.player.tickCount - startTick < 8)
				{
					return;
				}
				check(mc.player.getInventory().getItem(2).getCount() == 8, "Transferred stack must remain in inventory after server synchronization");
				click(38, 1, ClickType.THROW);
				startTick = mc.player.tickCount;
				advance(6);
			}
			case 6 ->
			{
				if (mc.player.tickCount - startTick < 8)
				{
					return;
				}
				check(mc.level.getEntitiesOfClass(ItemEntity.class, new AABB(6, 138, 6, 16, 146, 16)).stream().
						anyMatch(entity -> entity.getItem().getItem() == DemoContent.blockItem.get()), "Thrown item must exist in the client world");
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				serverAction = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					var drops = player.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(6, 138, 6, 16, 146, 16));
					check(!drops.isEmpty(), "Server drop must exist");
					for (ItemEntity drop : drops)
					{
						drop.setNoPickUpDelay();
						drop.playerTouch(player);
					}
					player.inventoryMenu.broadcastChanges();
				}, server);
				startTick = mc.player.tickCount;
				advance(7);
			}
			case 7 ->
			{
				if (mc.player.tickCount - startTick < 10)
				{
					return;
				}
				check(totalAnimated() == 18, "Vanilla pickup must restore all item counts");
				check(mc.level.getEntitiesOfClass(ItemEntity.class, new AABB(6, 138, 6, 16, 146, 16)).isEmpty(), "Picked-up item entity must disappear");
				PLibDatabase.LOGGER.info("ITEM IDENTITY WORLD OPERATIONS PASS: slots, hotbar, chest, both hands, count, split, merge, drop/pickup, inventory reopen");
				// These weak references do not retain either the manager or its controller.
				survivor = animated(1);
				survivorId = id(survivor);
				var manager = ((TestBlockItem)survivor.getItem()).getAnimationManager(AnimManagerKey.of(survivor));
				manager.bindModel(TestBlockItemRenderer.MODEL_DATA.getModel());
				managerReference = new WeakReference<>(manager);
				controllerReference = new WeakReference<>(manager.getControllers().get("default"));
				disconnectHooks = 0;
				mc.level.disconnect();
				mc.disconnect();
				advance(10);
			}
			case 11 ->
			{
				if (totalAnimated() != 18)
				{
					return;
				}
				check(totalAnimated() == 18, "Disposable world reentry must restore serialized item counts");
				ItemStack loaded = mc.player.getInventory().getItem(1);
				check(id(loaded) != survivorId, "Loaded stacks must have new runtime identities");
				var recreated = ((TestBlockItem)survivor.getItem()).getAnimationManager(AnimManagerKey.of(survivor));
				check(recreated.getControllers().get("default").getTime() == 0, "Cleared manager must restart animation on next use");
				PLibDatabase.LOGGER.info("ITEM IDENTITY REENTRY PASS: natural cleanup, collected controllers, new deserialized identities");
				disconnectHooks = 0;
				mc.level.disconnect();
				mc.disconnect();
				advance(13);
			}
			default ->
			{
			}
		}
	}

	private static void advance(int next)
	{
		phase = next;
		deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(2);
		PLibDatabase.LOGGER.info("ITEM IDENTITY PHASE {}", phase);
	}

	public static void observeDisconnectHook()
	{
		disconnectHooks++;
		PLibDatabase.LOGGER.info("ITEM IDENTITY DISCONNECT HOOK: phase {}, thread {}", phase, Thread.currentThread().getName());
	}

	private static void click(int slot, int button, ClickType type)
	{
		Minecraft mc = Minecraft.getInstance();
		mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, button, type, mc.player);
	}

	private static int totalAnimated()
	{
		var inventory = Minecraft.getInstance().player.getInventory();
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++)
		{
			ItemStack stack = inventory.getItem(slot);
			if (stack.getItem() == DemoContent.blockItem.get())
			{
				count += stack.getCount();
			}
		}
		return count;
	}

	private static ItemStack animated(int count)
	{
		return new ItemStack(DemoContent.blockItem.get(), count);
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
		return (Map<?, ?>)field(SingletonAnimationManager.class, "MANAGERS", null);
	}

	private static void pass(int number, String scenario)
	{
		PLibDatabase.LOGGER.info("ITEM IDENTITY REGRESSION {} PASS: {}", number, scenario);
	}

	private static void regressions()
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
		try
		{
			var server = Minecraft.getInstance().getSingleplayerServer();
			CompletableFuture.runAsync(() -> tool.hurtAndBreak(7, server.overworld(), null, broken ->
			{
				throw new AssertionError("Regression tool must not break");
			}), server).get(10, TimeUnit.SECONDS);
		}
		catch (Exception exception)
		{
			throw new AssertionError("Vanilla tool damage regression failed", exception);
		}
		check(tool.getDamageValue() == 24 && id(tool) == toolId, "Vanilla tool damage must retain identity");
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
		PLibDatabase.LOGGER.info("ITEM IDENTITY REGRESSIONS PASS: 16 cases, copy-sharing limitation, independent controllers and phases");
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
		ItemStack loaded = ItemStack.parse(registries, saved).orElseThrow();
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
		var firstManager = item.getAnimationManager(AnimManagerKey.of(first));
		var secondManager = item.getAnimationManager(AnimManagerKey.of(second));
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
		var copyManager = item.getAnimationManager(AnimManagerKey.of(first.copy()));
		check(copyManager == firstManager && copyManager.getControllers().get("default").getTime() == before,
				"Temporary copy must reuse the existing manager and phase");
		check(AnimManagerKey.of(first).key() < 0 && AnimManagerKey.of(Minecraft.getInstance().player).key() >= 0 &&
				AnimManagerKey.ofObject(item).key() >= 0, "Item keys must occupy a separate sign-bit domain");
		PLibDatabase.LOGGER.info("ITEM IDENTITY PHASE PROBE: IDs {} / {}, controller times {} / {}, copy reuses manager",
				id(first), id(second), firstController.getTime(), secondController.getTime());
	}
}
