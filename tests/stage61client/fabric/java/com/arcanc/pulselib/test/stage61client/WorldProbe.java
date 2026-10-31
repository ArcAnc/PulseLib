/**
 * @author ArcAnc
 * Created at: 31.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client;

import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.AnimManagerKey;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.animatable.singleton.SingletonAnimationManager;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationInstance;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.util.PLibDatabase;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Real level ticks, entity unload, expiry, reload and disconnect/reconnect witnesses. */
public final class WorldProbe
{
	private static final Identifier ID = PLibDatabase.rl("stage61world/player");
	private static final AnimManagerKey KEY = new AnimManagerKey(6162);
	private static int phase;
	private static int connections;
	private static int disconnects;
	private static int disconnectBarriers;
	private static int unloads;
	private static int expiries;
	private static int levelTicks;
	private static int base;
	private static int ownerBase;
	private static int sharedBase;
	private static float playerBase;
	private static int ticks;
	private static int stoppedOwnerTicks;
	private static boolean expired;
	private static boolean audited;
	private static @Nullable OwnerEntity owner;
	private static @Nullable OwnerEntity stale;
	private static @Nullable SharedOwner shared;
	private static @Nullable LocalPlayer previousPlayer;
	private static @Nullable PPlayerAnimationInstance instance;
	private static @Nullable PPlayerAnimationInstance previousInstance;
	private static @Nullable Object oldModel;
	private static @Nullable CompletableFuture<Void> reload;

	private WorldProbe()
	{
	}

	public static void animations(PPlayerAnimationRegistrar registrar)
	{
		registrar.register(ID, PPlayerAnimationDefinition.builder(ClientProbe.DATA).
				bind(PPlayerPart.ROOT, "root").mask(PPlayerPart.ROOT).
				controllers(controllers -> controllers.add("motion", () -> state -> state.controller().getState())).build());
	}

	public static void register()
	{
		ClientTickEvents.END_LEVEL_TICK.register(level ->
		{
			levelTicks++;
			if (shared != null)
				shared.getAnimationManager(KEY);
			if (owner != null && !owner.isRemoved())
				InstanceAnimationManager.addManager(owner.manager);
		});
		ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) ->
		{
			if (entity == owner)
				unloads++;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
		{
			disconnects++;
			// Production cleanup is queued first on this same executor during reentrant disconnects.
			client.execute(() -> disconnectBarriers++);
		});
		ClientTickEvents.START_CLIENT_TICK.register(mc ->
		{
			if (mc.level != null && stale != null && !expired && !cache(InstanceAnimationManager.class, "MANAGERS").containsKey(stale.manager))
			{
				check(mc.level.getGameTime() % 200 == 0, "expiry occurs at native 200-tick maintenance boundary");
				expired = true;
				expiries++;
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(WorldProbe::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
		{
			check(phase == 6 && disconnects == 2 && unloads == 2 && expiries == 2 && audited && empty(), "complete lifecycle before shutdown");
			PLibDatabase.LOGGER.info("STAGE61B2B WORLD SHUTDOWN PASS");
		});
	}

	private static void tick(Minecraft mc)
	{
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-world-lifecycle")), "disposable world client marker");
		if (++ticks > 2400)
			throw new AssertionError("STAGE61B2B timeout phase=" + phase);
		if (phase == 5)
		{
			if (mc.level != null || disconnects < connections || disconnectBarriers < connections)
				return;
			check(empty() && disconnects == connections && PPlayerAnimations.get(ID) != null, "native disconnect clears all caches and retains definitions");
			if (connections == 2)
			{
				phase = 6;
				PLibDatabase.LOGGER.info("STAGE61B2B WORLD PASS: connections=2, disconnects={}, unloads={}, expiryBoundaries={}, exactTickWindows=2, reloads=2, commonMixins=46, nativeTargets=39", disconnects, unloads, expiries);
				mc.stop();
				return;
			}
			ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString("127.0.0.1:25580"),
					new ServerData("Pulse disposable lifecycle", "127.0.0.1:25580", ServerData.Type.OTHER), false, null);
			phase = 0;
			return;
		}
		if (mc.player == null || mc.level == null || mc.getOverlay() != null || ClientProbe.DATA.getModel() == null)
			return;
		check(mc.player.isAlive() && mc.player.isCreative(), "isolated creative owner remains alive");
		if (phase == 0)
		{
			setup(mc);
			return;
		}
		if (phase == 1 && levelTicks - base >= 5)
		{
			base = levelTicks;
			ownerBase = owner.ticks;
			sharedBase = shared.ticks;
			playerBase = PPlayerAnimations.getHandle(mc.player, ID).controller("motion").getInterpolatedTime(1);
			phase = 2;
		}
		else if (phase == 2 && levelTicks - base >= 20)
		{
			int elapsed = levelTicks - base;
			check(owner.ticks - ownerBase == elapsed && shared.ticks - sharedBase == elapsed, "one instance/singleton tick per native level tick");
			check(Math.abs(PPlayerAnimations.getHandle(mc.player, ID).controller("motion").getInterpolatedTime(1) - playerBase - elapsed) < 0.00001f, "one player controller tick per native level tick");
			check(instance.player() == mc.player && instance.sampleFrame(1) != null, "native player owner/frame are current");
			if (!audited)
				auditMixins();
			mc.level.removeEntity(owner.getId(), Entity.RemovalReason.DISCARDED);
			check(unloads == connections && !cache(InstanceAnimationManager.class, "MANAGERS").containsKey(owner.manager), "real ClientLevel removal dispatch removes native entity manager");
			stoppedOwnerTicks = owner.ticks;
			oldModel = ClientProbe.DATA.getModel();
			reload = mc.reloadResourcePacks();
			phase = 3;
		}
		else if (phase == 3 && reload.isDone())
		{
			reload.join();
			check(ClientProbe.DATA.getModel() != oldModel, "native reload replaces baked model");
			check(PPlayerAnimations.getInstance(mc.player, ID) == instance, "reload retains player runtime; disconnects=" + disconnects + " connections=" + connections);
			base = levelTicks;
			phase = 4;
		}
		else if (phase == 4 && expired && levelTicks - base >= 5)
		{
			check(owner.ticks == stoppedOwnerTicks, "unloaded manager no longer ticks");
			InstanceAnimationManager.addManager(stale.manager);
			check(!empty() && !cache(InstanceAnimationManager.class, "MANAGERS").isEmpty() &&
					!cache(SingletonAnimationManager.class, "MANAGERS").isEmpty() && !cache(PPlayerAnimations.class, "INSTANCES").isEmpty(), "all caches populated before native disconnect");
			previousPlayer = mc.player;
			previousInstance = instance;
			shared = null;
			mc.getConnection().getConnection().disconnect(Component.literal("Pulse lifecycle probe"));
			mc.disconnect(new TitleScreen(), false);
			phase = 5;
		}
	}

	private static void setup(Minecraft mc)
	{
		connections++;
		instance = PPlayerAnimations.getInstance(mc.player, ID);
		check(instance != null && instance != previousInstance && instance.player() == mc.player, "fresh per-connection player manager");
		if (previousPlayer != null)
			check(previousPlayer != mc.player && previousPlayer.getUUID().equals(mc.player.getUUID()), "real reconnect retains UUID and replaces LocalPlayer");
		PPlayerAnimations.getHandle(mc.player, ID).play("motion", PRawAnimation.begin().thenLoop("world_tick").build());
		owner = new OwnerEntity(mc.level);
		owner.setId(0x616200 + connections);
		mc.level.addEntity(owner);
		owner.manager.initializeControllers();
		InstanceAnimationManager.addManager(owner.manager);
		stale = new OwnerEntity(mc.level);
		stale.manager.initializeControllers();
		try
		{
			Method seed = InstanceAnimationManager.class.getDeclaredMethod("addManager", PAnimationManager.class, long.class);
			seed.setAccessible(true);
			seed.invoke(null, stale.manager, 0L);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
		shared = new SharedOwner();
		shared.getAnimationManager(KEY);
		expired = false;
		base = levelTicks;
		phase = 1;
	}

	private static void auditMixins()
	{
		try (var stream = WorldProbe.class.getResourceAsStream("/assets/pulselib/lifecycle/mixin-targets.json");
			var reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			var entries = JsonParser.parseReader(reader).getAsJsonArray();
			Set<String> targets = new HashSet<>();
			Set<String> expected = new HashSet<>();
			Set<String> merged = new HashSet<>();
			for (var entry : entries)
			{
				expected.add("com.arcanc.pulselib.content.mixin." + entry.getAsJsonObject().get("mixin").getAsString());
				for (var target : entry.getAsJsonObject().getAsJsonArray("targets"))
					targets.add(target.getAsString());
			}
			for (String target : targets)
				for (var method : Class.forName(target, false, WorldProbe.class.getClassLoader()).getDeclaredMethods())
				{
					MixinMerged origin = method.getAnnotation(MixinMerged.class);
					if (origin != null)
						merged.add(origin.mixin());
				}
			check(merged.containsAll(expected), "every Common Mixin has a merged runtime method; missing=" + expected.stream().filter(name -> !merged.contains(name)).toList());
			MixinEnvironment.getCurrentEnvironment().audit();
			check(entries.size() == 46 && targets.size() == 39, "full Common Mixin target inventory");
			audited = true;
			PLibDatabase.LOGGER.info("STAGE61B2B MIXIN AUDIT PASS: mixins=46, targets=39");
		}
		catch (Exception exception)
		{
			throw new AssertionError("STAGE61B2B Mixin audit", exception);
		}
	}

	private static Map<?, ?> cache(Class<?> type, String name)
	{
		try
		{
			var field = type.getDeclaredField(name);
			field.setAccessible(true);
			return (Map<?, ?>)field.get(null);
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError(exception);
		}
	}

	private static boolean empty()
	{
		return cache(InstanceAnimationManager.class, "MANAGERS").isEmpty() &&
				cache(SingletonAnimationManager.class, "MANAGERS").isEmpty() && cache(PPlayerAnimations.class, "INSTANCES").isEmpty();
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE61B2B: " + message);
	}

	private static final class OwnerEntity extends ArmorStand implements PAnimatable<OwnerEntity>
	{
		private final InstanceAnimationManager<OwnerEntity> manager = new InstanceAnimationManager<>(this);
		private int ticks;

		private OwnerEntity(ClientLevel level)
		{
			super(EntityType.ARMOR_STAND, level);
			setInvisible(true);
			setNoGravity(true);
		}

		@Override
		public PAnimationManager<OwnerEntity> getAnimationManager(AnimManagerKey key)
		{
			return this.manager.getManager(key);
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<OwnerEntity> registrar)
		{
			registrar.add("tick", () -> state ->
			{
				this.ticks++;
				return ControllerState.STOP;
			});
		}
	}

	private static final class SharedOwner implements PAnimatable<SharedOwner>
	{
		private int ticks;

		@Override
		public PAnimationManager<SharedOwner> getAnimationManager(AnimManagerKey key)
		{
			return SingletonAnimationManager.getManager(key, this);
		}

		@Override
		public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<SharedOwner> registrar)
		{
			registrar.add("tick", () -> state ->
			{
				this.ticks++;
				return ControllerState.STOP;
			});
		}
	}
}
