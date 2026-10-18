/**
 * @author ArcAnc
 * Created at: 18.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.animatable.PAnimatable;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.animatable.PAnimationManager;
import com.arcanc.pulselib.content.animatable.instance.InstanceAnimationManager;
import com.arcanc.pulselib.content.model.animation.PAnimationEvaluationContext;
import com.arcanc.pulselib.content.model.animation.PInterpolationType;
import com.arcanc.pulselib.content.registration.block.block_entity.TestBlockEntity;
import com.arcanc.pulselib.content.registration.entity.TestEntity;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.MolangParser;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.helpers.PLibHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import org.joml.Quaternionf;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.arcanc.pulselib.test.SmokeHarness.check;
import static com.arcanc.pulselib.test.SmokeHarness.field;

/** Opt-in smoke assertions use loaded Gecko assets and real renderer-owned managers. */
public final class AnimationSamplingHarness
{
	public static final PModelData MODEL_DATA = PModelData.entity(PLibDatabase.rl("sampling/test_entity"), PGeckoModelLoader.INSTANCE.id());
	private static final Map<PAnimationManager<?>, PAnimationController<?>> WATCHED = new IdentityHashMap<>();
	private static long start;
	private static long lastCleanupTick = -1;
	private static int cleanupCycles;
	private static boolean lifecycleComplete;

	private AnimationSamplingHarness()
	{
	}

	public static void checkTime(PAnimationController<?> controller, MolangParser.Context context, float partialTick)
	{
		check(Math.abs(MolangParser.evaluate("q.anim_time", context) - controller.getInterpolatedTime(partialTick) / 20f) < 0.000001f,
				"Renderer/player Molang time must use interpolated seconds");
	}

	static void run()
	{
		var model = MODEL_DATA.getModel();
		check(model != null && model.animations().containsKey("turn720"), "Sampling Gecko assets must load");
		for (int turns : new int[]{360, 720})
		{
			for (int eighth = 0; eighth <= 8; eighth++)
			{
				float alpha = eighth / 8f;
				Quaternionf actual = model.animations().get("turn" + turns).
						calculateBoneTransformations("body", alpha * 40, PInterpolationType.LINEAR).rotation();
				Quaternionf expected = new Quaternionf().rotationY((float)Math.toRadians(alpha * turns));
				check(Math.abs(Math.abs(expected.dot(actual)) - 1) < 0.00001f, "Gecko full turn sampled pose at " + alpha);
			}
		}
		float[] references = {0, 0.5234375f, 1.1875f, 1.7578125f, 2};
		for (int quarter = 0; quarter <= 4; quarter++)
		{
			float value = model.animations().get("curve").calculateBoneTransformations("body", 20 + quarter * 10, PInterpolationType.LINEAR).translation().x;
			check(Math.abs(value - references[quarter]) < 0.000001f, "Gecko uniform Catmull-Rom reference at " + quarter);
		}
		for (int frame = 0; frame < 100; frame++)
		{
			for (float time : new float[]{10 + frame * 0.01f, 30 + frame * 0.01f})
			{
				var context = new MolangParser.Context().query("anim_time", PAnimationEvaluationContext.ticksToSeconds(time));
				float value = model.animations().get("clock").calculateBoneTransformations("body", 0, PInterpolationType.LINEAR, context).translation().x;
				check(Math.abs(value - time / 320f) < 0.000001f, "Independent interpolated Molang seconds");
			}
		}
		PLibDatabase.LOGGER.info("PULSE TEST M-A3/M-A4/H2/H1 PASS: loaded Gecko 360/720 poses, uniform spline references and independent Molang contexts");
	}

	/** Waits for two real scheduled cleanup cycles while Entity and BlockEntity keep rendering. */
	static boolean frameWorld()
	{
		if (lifecycleComplete)
		{
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		if (start == 0)
		{
			DisposableTestWorlds.requireCurrentWorld();
			for (var entity : mc.level.entitiesForRendering())
			{
				if (entity instanceof TestEntity owner)
				{
					watch(owner);
				}
			}
			if (mc.level.getBlockEntity(new BlockPos(11, 140, 14)) instanceof TestBlockEntity owner)
			{
				watch(owner);
			}
			check(WATCHED.size() >= 2, "L25 requires rendered Entity and BlockEntity managers");
			start = System.nanoTime();
		}
		long tick = mc.level.getGameTime();
		if (tick % 200 == 0 && tick != lastCleanupTick)
		{
			lastCleanupTick = tick;
			cleanupCycles++;
		}
		Map<?, ?> registry = (Map<?, ?>)field(InstanceAnimationManager.class, "MANAGERS", null);
		for (var entry : WATCHED.entrySet())
		{
			check(registry.containsKey(entry.getKey()), "L25 active renderer-owned manager expired");
			check(entry.getKey().getControllers().containsValue(entry.getValue()), "L25 recreated a controller");
			check(MolangParser.evaluate("v.l25_probe", entry.getValue().persistentMolangContext()) == 42, "L25 reset persistent controller state");
		}
		check(System.nanoTime() - start < 90_000_000_000L, "L25 world lifecycle smoke timed out");
		if (System.nanoTime() - start < 21_000_000_000L || cleanupCycles < 2)
		{
			return false;
		}
		lifecycleComplete = true;
		PLibDatabase.LOGGER.info("PULSE TEST L25 WORLD PASS: Entity/BlockEntity retain controllers and Molang state over {} cleanup cycles", cleanupCycles);
		return true;
	}

	/** Exercises the real client entity-unload and chunk-drop paths in the marked test save. */
	static void verifyRemovalHooks()
	{
		if (WATCHED.isEmpty())
		{
			return;
		}
		DisposableTestWorlds.requireCurrentWorld();
		Minecraft mc = Minecraft.getInstance();
		Map<?, ?> registry = (Map<?, ?>)field(InstanceAnimationManager.class, "MANAGERS", null);
		for (var manager : WATCHED.keySet())
		{
			// Later pixel cases can cull a fixture; register it before testing its removal hook.
			InstanceAnimationManager.addManager(manager);
			check(registry.containsKey(manager), "Removal fixture must be registered");
			if (manager.getAnimatable() instanceof TestEntity entity)
			{
				mc.level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
			}
			else if (manager.getAnimatable() instanceof TestBlockEntity block)
			{
				mc.level.getChunkSource().drop(new ChunkPos(block.getBlockPos()));
			}
			check(!registry.containsKey(manager), "L25 entity/chunk unload must remove its manager");
		}
		WATCHED.clear();
		PLibDatabase.LOGGER.info("PULSE TEST L25 REMOVAL PASS: entity removal and client chunk unload hooks release their managers");
	}

	private static <T extends PAnimatable<T>> void watch(T owner)
	{
		var manager = PLibHelper.getManager(owner);
		var controller = manager.getControllers().values().iterator().next();
		controller.persistentMolangContext().variable("l25_probe", 42);
		WATCHED.put(manager, controller);
	}
}
