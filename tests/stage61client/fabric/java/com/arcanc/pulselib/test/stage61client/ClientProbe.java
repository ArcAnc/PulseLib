/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage61client;

import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.platform.PulseServices;
import com.arcanc.pulselib.plugin.PulsePluginManager;
import com.arcanc.pulselib.util.PLibDatabase;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Proves startup collection, shared CPU model bake and a real second resource reload. */
public final class ClientProbe implements ClientModInitializer
{
	public static final PModelData DATA = PModelData.direct(PLibDatabase.rl("lifecycle/probe"), PGeckoModelLoader.INSTANCE.id());
	public static int common;
	public static int client;
	public static int models;
	private static @Nullable PBakedModel first;
	private static @Nullable CompletableFuture<Void> reload;
	private static int before;
	private static int ticks;
	private static boolean completed;

	@Override
	public void onInitializeClient()
	{
		if (Boolean.getBoolean("pulselib.fabricStagesProbe"))
		{
			WorldStageProbe.register();
			return;
		}
		if (Boolean.getBoolean("pulselib.fabricWorldProbe"))
		{
			WorldProbe.register();
			return;
		}
		if (Boolean.getBoolean("pulselib.fabricAtlasProbe"))
		{
			AtlasProbe.register();
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(ClientProbe::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft ->
		{
			check(completed, "both startup/reload witnesses complete before normal shutdown");
			PLibDatabase.LOGGER.info("STAGE61B1 CLIENT SHUTDOWN PASS");
		});
	}

	private static void tick(Minecraft minecraft)
	{
		check(Files.isRegularFile(minecraft.gameDirectory.toPath().resolve(".pulselib-client-lifecycle")), "disposable client marker");
		if (++ticks > 2400)
			throw new AssertionError("STAGE61B1 client resource reload timeout");
		if (minecraft.getOverlay() != null || DATA.getModel() == null)
			return;
		check(PulseServices.PLATFORM.isClient() && common == 1 && client == 1, "physical client and once-only ordered registration");
		check(PulsePluginManager.get().pluginIds().equals(List.of(PLibDatabase.rl("stage61client/client"), PLibDatabase.rl("stage61client/common"))), "CLIENT/BOTH IDs and SERVER side filtering");
		check(DATA.getModel().bones().size() == 1 && DATA.getModel().bones().getFirst().name().equals("root"), "actual decoded/baked bone");
		if (reload == null)
		{
			verifyAccessRules();
			first = DATA.getModel();
			before = models;
			check(before > 0, "startup model declarations were collected before model loading");
			reload = minecraft.reloadResourcePacks();
			return;
		}
		if (!reload.isDone())
			return;
		reload.join();
		check(DATA.getModel() != first && models == before + 1, "reload recollects models exactly once and replaces baked model");
		completed = true;
		PLibDatabase.LOGGER.info("STAGE61B1 CLIENT RELOAD PASS: common={}, client={}, modelCollections={}, decodedModels=1, reloads=1, freshModel=true, accessRules=6", common, client, models);
		minecraft.stop();
	}

	private static void verifyAccessRules()
	{
		try
		{
			ClassLoader loader = ClientProbe.class.getClassLoader();
			String[] types = {"net.minecraft.client.model.QuadrupedModel",
					"net.minecraft.client.model.geom.builders.CubeDeformation",
					"net.minecraft.client.renderer.RenderPipelines"};
			String[][] fields = {{"body"}, {"growX", "growY", "growZ"}, {"MATRICES_PROJECTION_SNIPPET"}};
			for (int index = 0; index < types.length; index++)
				for (String field : fields[index])
					check(Modifier.isPublic(Class.forName(types[index], false, loader).getDeclaredField(field).getModifiers()), "access-widened field " + field);
			Class<?> type = Class.forName("net.minecraft.client.renderer.rendertype.RenderType", false, loader);
			Class<?> setup = Class.forName("net.minecraft.client.renderer.rendertype.RenderSetup", false, loader);
			check(Modifier.isPublic(type.getDeclaredMethod("create", String.class, setup).getModifiers()), "access-widened RenderType.create signature");
		}
		catch (ReflectiveOperationException exception)
		{
			throw new AssertionError("STAGE61B1 access rule target missing", exception);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE61B1: " + message);
	}
}
