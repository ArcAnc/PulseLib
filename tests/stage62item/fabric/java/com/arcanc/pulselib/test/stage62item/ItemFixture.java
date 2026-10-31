/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage62item;

import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.animatable.PItemStackAnimationData;
import com.arcanc.pulselib.util.PLibDatabase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;

/** Disposable content exists on both endpoints; the dedicated server never creates client identities. */
public final class ItemFixture implements ModInitializer
{
	public static final AnimatedItem ITEM = new AnimatedItem();
	private static boolean moved;

	@Override
	public void onInitialize()
	{
		Registry.register(BuiltInRegistries.ITEM, PLibDatabase.rl("stage62item"), ITEM);
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
		{
			if (!server.getWorldData().getLevelName().equals("fabric-item-consumers"))
				throw new AssertionError("Item fixture requires its disposable named world");
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
		{
			handler.player.setGameMode(GameType.CREATIVE);
			server.execute(() ->
			{
				moved = false;
				handler.player.getInventory().clearContent();
				var contents = new SimpleContainer(27);
				var stack = new ItemStack(ITEM, 8);
				if ((Object)stack instanceof PItemStackAnimationData)
					throw new AssertionError("Client identity Mixin loaded on dedicated server");
				contents.setItem(0, stack);
				handler.player.openMenu(new SimpleMenuProvider((id, inventory, player) -> ChestMenu.threeRows(id, inventory, contents), Component.literal("PulseLib disposable item menu")));
			});
		});
		ServerTickEvents.END_SERVER_TICK.register(server ->
		{
			for (var player : server.getPlayerList().getPlayers())
				if (!moved && player.containerMenu instanceof ChestMenu menu && menu.getSlot(0).getItem().isEmpty())
				{
					int count = 0;
					for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
						if (player.getInventory().getItem(slot).is(ITEM))
							count += player.getInventory().getItem(slot).getCount();
					if (count != 8)
						throw new AssertionError("Native menu transfer lost item counts: " + count);
					moved = true;
					PLibDatabase.LOGGER.info("STAGE62C2 SERVER MENU PASS: real quick-move packet, count=8, no client identity");
					// Force a normal content packet after server-side validation, then a fresh network object on the client.
					menu.sendAllDataToRemote();
				}
		});
	}

	public static final class AnimatedItem extends Item implements PAnimatable<AnimatedItem>
	{
		private AnimatedItem()
		{
			super(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, PLibDatabase.rl("stage62item"))));
		}

		@Override
		public PAnimationManager<AnimatedItem> getAnimationManager(AnimManagerKey key)
		{
			return SingletonAnimationManager.getManager(key, this);
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<AnimatedItem> registrar)
		{
			registrar.add(() -> state -> ControllerState.PLAY);
		}
	}
}
