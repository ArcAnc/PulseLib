/**
 * @author ArcAnc
 * Created at: 03.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.block.block_entity.ber.TestBlockEntityRenderer;
import com.arcanc.pulselib.content.registration.entity.renderer.TestEntityRender;
import com.arcanc.pulselib.content.registration.item.renderer.TestBlockItemRenderer;
import com.arcanc.pulselib.content.registration.player.PPlayerAcrobaticDemo;
import com.arcanc.pulselib.content.registration.player.PPlayerBallDemo;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PResourceCache;
import com.google.gson.JsonParser;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/** Packaged client plugin/assets, fresh integrated world and a real resource reload. */

public final class DemoProbe
{
	private static int ticks;
	private static int phase;
	private static int assets;
	private static boolean scene;
	private static volatile net.minecraft.core.BlockPos sceneBlock = net.minecraft.core.BlockPos.ZERO;
	private static int frames;
	private static int epochs;
	private static int entityBefore, blockBefore, itemBefore;
	private static boolean completed;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable PBakedModel previous;

	private DemoProbe()
	{
	}

	public static void tick(Minecraft mc)
	{
		if (H14OffscreenHarness.enabled() || completed)
			return;
		DemoChecks.runDirectory();
		if (phase == 5)
		{
			if (SmokeHarness.completed())
			{
				createScene(mc);
				phase = 3;
			}
			return;
		}
		if (phase == 4)
		{
			if (SmokeLifecycleHarness.frame())
				complete(mc);
			return;
		}
		if (++ticks > 3000)
			throw new AssertionError("PULSE DEMO client deadline phase=" + phase);
		if (mc.getOverlay() != null || ClientTestPlugin.GLTF.getModel() == null || ClientTestPlugin.GECKO.getModel() == null)
			return;
		if (phase == 0)
		{
			DemoChecks.plugins(true);
			DemoChecks.check(ClientTestPlugin.constructions == 1 && ClientTestPlugin.models == 1, "Client model registration before bootstrap");
			models(mc);
			previous = ClientTestPlugin.GLTF.getModel();
			DisposableDemoWorlds.create();
			phase = 1;
		}
		else if (phase == 1 && mc.level != null && mc.player != null && mc.getSingleplayerServer() != null)
		{
			DemoChecks.world(mc.getSingleplayerServer(), true);
			PLibDatabase.LOGGER.info("PULSE DEMO WORLD PASS: {}", mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT));
			if (SmokeHarness.enabled())
			{
				phase = 5;
				SmokeHarness.begin();
			}
			else
			{
				createScene(mc);
				phase = 3;
			}
		}
		else if (phase == 3 && scene && ++frames >= 80)
		{
			if (SmokeHarness.enabled() && !SmokeHarness.sceneReleased(mc))
				return;
			if (Boolean.getBoolean("pulselib.test.animationRegression") && epochs == 0 && !AnimationSamplingHarness.frameWorld())
				return;
			verifyScene();
			epochs++;
			if (epochs == 1)
			{
				reload = mc.reloadResourcePacks();
				phase = 2;
			}
			else
				finishSuites(mc);
		}
		else if (phase == 2 && reload != null && reload.isDone())
		{
			reload.join();
			DemoChecks.check(ClientTestPlugin.models == 2 && ClientTestPlugin.GLTF.getModel() != previous,
					"Resource reload replaces baked models from packaged assets");
			models(mc);
			PPlayerAcrobaticDemo.play(mc.player);
			DemoChecks.check(PPlayerAnimations.getHandle(mc.player, PPlayerAcrobaticDemo.ID).isPlaying("acrobatic"), "Acrobatic demo starts through canonical handle");
			beginEpoch();
			phase = 3;
		}
	}

	/** Keeps the independent content epochs deterministic after Smoke releases its world scene. */
	public static void lockSceneCamera()
	{
		Minecraft mc = Minecraft.getInstance();
		if (!SmokeHarness.enabled() || !scene || phase >= 4 || mc.player == null)
			return;
		net.minecraft.client.KeyMapping.releaseAll();
		mc.player.setYRot(0);
		mc.player.yRotO = 0;
		mc.player.yBodyRot = 0;
		mc.player.yBodyRotO = 0;
		mc.player.setXRot(8);
		mc.player.xRotO = 8;
	}

	private static void finishSuites(Minecraft mc)
	{
		if (Boolean.getBoolean("pulselib.test.animationRegression"))
		{
			AnimationSamplingHarness.verifyRemovalHooks();
			PLibDatabase.LOGGER.info("PULSE ANIMATION COMPLETE: assertions={}, maintenance={}", RegressionChecks.assertions, RegressionChecks.maintenancePasses);
		}
		if (Boolean.getBoolean("pulselib.test.lifecycleRegression"))
		{
			phase = 4;
			SmokeLifecycleHarness.begin();
		}
		else
			complete(mc);
	}

	private static void complete(Minecraft mc)
	{
		completed = true;
		var result = new java.util.LinkedHashMap<String, Object>();
		result.putAll(Map.of("status", "PASS", "side", "client", "candidates", 3, "loaded", 2,
				"origin", DemoChecks.jar(ClientTestPlugin.class), "assetChecks", assets, "modelEpochs", epochs,
				"animation", Map.of("enabled", Boolean.getBoolean("pulselib.test.animationRegression"), "assertions", RegressionChecks.assertions, "maintenance", RegressionChecks.maintenancePasses),
				"lifecycle", Boolean.getBoolean("pulselib.test.lifecycleRegression") ? SmokeLifecycleHarness.result() : Map.of("enabled", false),
				"renders", java.util.List.of(TestEntityRender.renders, TestBlockEntityRenderer.renders, TestBlockItemRenderer.renders)));
		result.put("smoke", SmokeHarness.result());
		DemoChecks.writeResult(result);
		PLibDatabase.LOGGER.info("PULSE DEMO CLIENT COMPLETE: assets={}, epochs={}, player demos=2", assets, epochs);
		mc.stop();
	}

	static net.minecraft.core.BlockPos sceneBlockPosition()
	{
		return sceneBlock;
	}

	private static void createScene(Minecraft mc)
	{
		mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		mc.player.setYRot(0);
		mc.player.setXRot(8);
		var server = mc.getSingleplayerServer();
		server.execute(() ->
		{
			var player = server.getPlayerList().getPlayers().getFirst();
			var level = player.level();
			var pos = player.blockPosition().offset(0, 0, 4);
			sceneBlock = pos.immutable();
			level.setBlockAndUpdate(pos, DemoContent.block.get().defaultBlockState());
			((TestBlockEntity)level.getBlockEntity(pos)).changePlayAnimation();
			var entity = DemoContent.entity.get().create(level, EntitySpawnReason.COMMAND);
			entity.setPos(pos.getX() + 2, pos.getY(), pos.getZ());
			entity.setNoAi(true);
			entity.showArmor = true;
			if (SmokeHarness.enabled())
			{
				PLibDatabase.LOGGER.info("PULSE DEMO COLLISION BEFORE RELEASE: free={}, box={}", level.noCollision(entity), entity.getBoundingBox());
				SmokeHarness.releaseScene(level);
				DemoChecks.check(level.noCollision(entity), "Independent demo actor must spawn without block collision");
				PLibDatabase.LOGGER.info("PULSE DEMO COLLISION AFTER RELEASE: free={}", level.noCollision(entity));
			}
			level.addFreshEntity(entity);
			PLibDatabase.LOGGER.info("PULSE DEMO SPAWN: id={}, entity={}", entity.getId(), entity);
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DemoContent.blockItem.get()));
			player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(DemoContent.tail.get()));
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(DemoContent.helmet.get()));
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DemoContent.chestplate.get()));
			player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(DemoContent.leggings.get()));
		});
		DemoChecks.check(PPlayerAnimations.getHandle(mc.player,
				PPlayerBallDemo.ID) != null, "Ball demo registered for actual local player");
		DemoChecks.check(PPlayerAnimations.getHandle(mc.player,
				PPlayerAcrobaticDemo.ID) != null, "Acrobatic demo registered for actual local player");
		PPlayerBallDemo.play(mc.player);
		DemoChecks.check(PPlayerAnimations.getHandle(mc.player, PPlayerBallDemo.ID).isPlaying("ball_toss"), "Ball demo starts through canonical handle");
		scene = true;
		beginEpoch();
	}

	private static void beginEpoch()
	{
		frames = 0;
		entityBefore = TestEntityRender.renders;
		blockBefore = TestBlockEntityRenderer.renders;
		itemBefore = TestBlockItemRenderer.renders;
	}

	private static void verifyScene()
	{
		Minecraft mc = Minecraft.getInstance();
		var positions = new java.util.ArrayList<net.minecraft.world.phys.Vec3>();
		for (var entity : mc.level.entitiesForRendering())
			if (entity instanceof com.arcanc.pulselib.content.registration.entity.TestEntity)
				positions.add(entity.position());
		PLibDatabase.LOGGER.info("PULSE DEMO SCENE PROBE: epoch={}, deltas={}/{}/{}, camera={}, eye={}, yaw={}, pitch={}, player={}, entities={}",
				epochs + 1, TestEntityRender.renders - entityBefore, TestBlockEntityRenderer.renders - blockBefore,
				TestBlockItemRenderer.renders - itemBefore, mc.options.getCameraType(), mc.gameRenderer.getMainCamera().position(),
				mc.player.getYRot(), mc.player.getXRot(), mc.player.position(), positions);
		DemoChecks.check(TestEntityRender.renders > entityBefore + 20,
				"Canonical entity meshes submit in each resource epoch");
		DemoChecks.check(TestBlockEntityRenderer.renders > blockBefore + 20,
				"Canonical block meshes submit in each resource epoch");
		DemoChecks.check(TestBlockItemRenderer.renders > itemBefore + 20,
				"Canonical held item special renderer submits in each resource epoch");
		DemoChecks.check(com.arcanc.pulselib.content.registration.item.TestArmor.setups > 0, "Canonical armor provider executes native setup");
		DemoChecks.check(com.arcanc.pulselib.content.registration.item.TestTailItem.controllerSamples > 0, "Canonical tail attachment controller sampled");
		DemoChecks.check(ClientTestPlugin.animatedRenders > 0, "Canonical ball hand socket callback executed");
		PLibDatabase.LOGGER.info("PULSE DEMO SCENE PASS: epoch={}, entity={}, block={}, item={}", epochs + 1,
				TestEntityRender.renders - entityBefore,
				TestBlockEntityRenderer.renders - blockBefore,
				TestBlockItemRenderer.renders - itemBefore);
	}

	private static void models(Minecraft mc)
	{
		if (Boolean.getBoolean("pulselib.test.animationRegression"))
		{
			AnimationLifecycleHarness.run(TestEntityRender.MODEL_DATA.getModel());
			AnimationSamplingHarness.run();
		}
		var gltf = ClientTestPlugin.GLTF.getModel();
		DemoChecks.check(IntStream.range(0, gltf.boneCount()).
				mapToObj(gltf::bone).
				anyMatch(bone -> !bone.meshes().isEmpty()), "Real canonical GLB mesh is baked");
		DemoChecks.check(ClientTestPlugin.GECKO.getModel().
				boneCount() > 0 && !ClientTestPlugin.GECKO.getModel().
				animations().
				isEmpty(), "Canonical Gecko rig/animation is decoded");
		try (var stream = DemoProbe.class.getResourceAsStream("/pulselib-demo-assets.json"))
		{
			var manifest = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).
					getAsJsonObject();
			for (var entry : manifest.entrySet())
			{
				var resource = mc.getResourceManager().
						getResource(Identifier.parse(entry.getKey())).orElseThrow();
				DemoChecks.check(resource.sourcePackId().contains(DemoChecks.MOD_ID), "Asset is supplied by packaged bootstrap mod: " + resource.sourcePackId());
				try (var input = resource.open())
				{
					DemoChecks.check(HexFormat.of().
							formatHex(MessageDigest.getInstance("SHA-256").
							digest(input.readAllBytes())).
							equals(entry.getValue().
							getAsString()), "Actual packaged asset bytes equal pinned canonical source");
				}
				assets++;
			}
		}
		catch (Exception exception)
		{
			throw new IllegalStateException(exception);
		}
		DemoChecks.check(!PResourceCache.getTextureAtlas().
				getSprite(PResourceCache.spriteId(PLibDatabase.rl("item/test_block/pyramid"))).
				contents().name().getPath().equals("missingno"), "Actual canonical atlas texture present");
		PLibDatabase.LOGGER.info("PULSE DEMO ASSETS PASS: checks={}, origin={}", assets, DemoChecks.jar(ClientTestPlugin.class));
	}

	public static void stopped()
	{
		if (H14OffscreenHarness.enabled())
		{
			H14OffscreenHarness.stopped();
			return;
		}
		DemoChecks.check(completed && assets == 100, "Two canonical demo asset epochs completed before client shutdown");
		PLibDatabase.LOGGER.info("PULSE DEMO CLIENT SHUTDOWN PASS");
	}
}
