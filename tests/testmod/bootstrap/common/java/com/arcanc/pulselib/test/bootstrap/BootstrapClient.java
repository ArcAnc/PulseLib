/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.bootstrap;

import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PResourceCache;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/** Packaged client plugin/assets, fresh integrated world and a real resource reload. */
public final class BootstrapClient
{
	private static int ticks;
	private static int phase;
	private static int assets;
	private static boolean completed;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable PBakedModel previous;

	private BootstrapClient()
	{
	}

	public static void tick(Minecraft mc)
	{
		BootstrapChecks.runDirectory();
		if (++ticks > 3000)
			throw new AssertionError("PULSE BOOTSTRAP client deadline phase=" + phase);
		if (mc.getOverlay() != null || ClientTestPlugin.GLTF.getModel() == null || ClientTestPlugin.GECKO.getModel() == null)
			return;
		if (phase == 0)
		{
			BootstrapChecks.plugins(true);
			BootstrapChecks.check(ClientTestPlugin.constructions == 1 && ClientTestPlugin.models == 1, "Client model registration before bootstrap");
			models(mc);
			previous = ClientTestPlugin.GLTF.getModel();
			DisposableTestWorlds.create();
			phase = 1;
		}
		else if (phase == 1 && mc.level != null && mc.player != null && mc.getSingleplayerServer() != null)
		{
			BootstrapChecks.world(mc.getSingleplayerServer(), true);
			PLibDatabase.LOGGER.info("PULSE BOOTSTRAP WORLD PASS: {}", mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
			reload = mc.reloadResourcePacks();
			phase = 2;
		}
		else if (phase == 2 && reload != null && reload.isDone())
		{
			reload.join();
			BootstrapChecks.check(ClientTestPlugin.models == 2 && ClientTestPlugin.GLTF.getModel() != previous, "Resource reload replaces baked models from packaged assets");
			models(mc);
			BootstrapChecks.world(mc.getSingleplayerServer(), true);
			completed = true;
			BootstrapChecks.writeResult(Map.of("status", "PASS", "side", "client", "candidates", 3, "loaded", 2,
					"origin", BootstrapChecks.jar(ClientTestPlugin.class), "assetChecks", assets, "modelEpochs", 2,
					"gltfBones", ClientTestPlugin.GLTF.getModel().boneCount(),
					"geckoBones", ClientTestPlugin.GECKO.getModel().boneCount(),
					"geckoAnimations", ClientTestPlugin.GECKO.getModel().animations().size()));
			PLibDatabase.LOGGER.info("PULSE BOOTSTRAP CLIENT COMPLETE: assets={}, epochs=2, gltfBones={}, geckoBones={}, geckoAnimations={}",
					assets, ClientTestPlugin.GLTF.getModel().boneCount(),
					ClientTestPlugin.GECKO.getModel().boneCount(), ClientTestPlugin.GECKO.getModel().animations().size());
			mc.stop();
		}
	}

	private static void models(Minecraft mc)
	{
		var gltf = ClientTestPlugin.GLTF.getModel();
		BootstrapChecks.check(IntStream.range(0, gltf.boneCount()).
				mapToObj(gltf::bone).
				anyMatch(bone -> !bone.meshes().isEmpty()), "Real canonical GLB mesh is baked");
		BootstrapChecks.check(ClientTestPlugin.GECKO.getModel().boneCount() > 0 && !ClientTestPlugin.GECKO.getModel().animations().isEmpty(), "Canonical Gecko rig/animation is decoded");
		try (var stream = BootstrapClient.class.getResourceAsStream("/pulselib-bootstrap-assets.json"))
		{
			var manifest = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).
					getAsJsonObject();
			for (var entry : manifest.entrySet())
			{
				var resource = mc.getResourceManager().
						getResource(Identifier.parse(entry.getKey())).orElseThrow();
				BootstrapChecks.check(resource.sourcePackId().contains(BootstrapChecks.MOD_ID), "Asset is supplied by packaged bootstrap mod: " + resource.sourcePackId());
				try (var input = resource.open())
				{
					BootstrapChecks.check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())).equals(entry.getValue().getAsString()), "Actual packaged asset bytes equal pinned canonical source");
				}
				assets++;
			}
		}
		catch (Exception exception)
		{
			throw new IllegalStateException(exception);
		}
		BootstrapChecks.check(!PResourceCache.getTextureAtlas().
				getSprite(PResourceCache.spriteId(PLibDatabase.rl("item/test_block/pyramid"))).
				contents().name().getPath().equals("missingno"), "Actual canonical atlas texture present");
		PLibDatabase.LOGGER.info("PULSE BOOTSTRAP ASSETS PASS: checks={}, origin={}", assets, BootstrapChecks.jar(ClientTestPlugin.class));
	}

	public static void stopped()
	{
		BootstrapChecks.check(completed && assets == 12, "Two packaged asset epochs completed before client shutdown");
		PLibDatabase.LOGGER.info("PULSE BOOTSTRAP CLIENT SHUTDOWN PASS");
	}
}
