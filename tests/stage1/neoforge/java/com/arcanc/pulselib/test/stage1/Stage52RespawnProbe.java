/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.PAnimationController;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.baked.PBakedModel;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationInstance;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationFrame;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Actual packet-driven LocalPlayer replacement and resource reload witnesses. */
public final class Stage52RespawnProbe
{
	private static final List<PModelData> MODELS = List.of(
			PModelData.direct(PLibDatabase.rl("player/regression/motion"), PGeckoModelLoader.INSTANCE.id()),
			PModelData.direct(PLibDatabase.rl("player/regression/motion_gltf"), PGltfModelLoader.INSTANCE.id()));
	private static final Identifier[] IDS = {PLibDatabase.rl("stage525b/gecko"), PLibDatabase.rl("stage525b/gltf")};
	private static final @Nullable PPlayerAnimationInstance[] INSTANCES = new PPlayerAnimationInstance[2];
	private static final @Nullable PPlayerAnimationFrame[] FRAMES = new PPlayerAnimationFrame[2];
	private static final @Nullable Object[] CONTROLLERS = new Object[2];
	private static final @Nullable PBakedModel[] BEFORE_RELOAD = new PBakedModel[2];
	private static @Nullable LocalPlayer previous;
	private static @Nullable CompletableFuture<Void> reload;
	private static int phase;
	private static int ticks;
	private static int settled;
	private static int suites;

	private Stage52RespawnProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) ->
		{
			check(phase == 7 && suites == 4, "all respawn/reload suites complete before shutdown");
			PLibDatabase.LOGGER.info("STAGE525B SHUTDOWN PASS");
		});
	}

	public static void models(PModelRegistrar registrar)
	{
		for (PModelData model : MODELS)
			registrar.model(model).texture("0", PLibDatabase.rl("stage42a_white"));
	}

	public static void animations(PPlayerAnimationRegistrar registrar)
	{
		for (int index = 0; index < MODELS.size(); index++)
			registrar.register(IDS[index], PPlayerAnimationDefinition.builder(MODELS.get(index)).
					bind(PPlayerPart.ROOT, "root").
					mask(index == 0 ? PPlayerPart.ROOT : PPlayerPart.BODY).
					controllers(controllers -> controllers.add("motion", () -> state ->
							state.controller().getState())).
					build());
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (++ticks > 2400)
			throw new AssertionError("STAGE525B respawn/reload timeout phase=" + phase);
		check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-respawn")), "disposable client marker");
		if (mc.player == null || mc.level == null || mc.getOverlay() != null || MODELS.stream().anyMatch(model -> model.getModel() == null))
			return;
		if (phase == 0)
		{
			if (mc.player.tickCount < 20)
				return;
			for (int index = 0; index < IDS.length; index++)
			{
				var handle = PPlayerAnimations.getHandle(mc.player, IDS[index]);
				check(handle.play("motion", PRawAnimation.begin().thenLoop("A").build()), "real controller starts");
				PAnimationController<PPlayerAnimationInstance> controller = handle.controller("motion");
				controller.seek(MODELS.get(index).getModel(), 3 + 4 * index);
				controller.pause();
				INSTANCES[index] = PPlayerAnimations.getInstance(mc.player, IDS[index]);
				CONTROLLERS[index] = controller;
				BEFORE_RELOAD[index] = MODELS.get(index).getModel();
			}
			phase = 1;
			return;
		}
		if (phase == 1 || phase == 3 || phase == 6)
		{
			if (++settled < 10)
				return;
			settled = 0;
			verify(mc);
			if (phase == 3)
			{
				reload = mc.reloadResourcePacks();
				phase = 4;
				return;
			}
			if (phase == 6)
			{
				check(suites == 4, "all before/after respawn/reload suites");
				PLibDatabase.LOGGER.info("STAGE525B CLIENT PASS: respawns=2, suites={}, formats=Gecko/glTF, definitions=2, stable instances/controllers, owner refresh, model reload, physics unchanged", suites);
				phase = 7;
				mc.stop();
				return;
			}
			previous = mc.player;
			mc.getConnection().sendCommand("pulse_respawn_probe");
			phase = 2;
			return;
		}
		if (phase == 2 || phase == 5)
		{
			if (mc.player == previous)
				return;
			check(mc.player.getUUID().equals(previous.getUUID()), "actual packet-driven replacement retains UUID");
			check(mc.level.getEntity(previous.getId()) != previous, "old client entity removed");
			phase = phase == 2 ? 3 : 6;
			return;
		}
		if (phase == 4 && reload.isDone())
		{
			reload.join();
			if (++settled < 10)
				return;
			settled = 0;
			for (int index = 0; index < IDS.length; index++)
				check(MODELS.get(index).getModel() != BEFORE_RELOAD[index], "real resource reload replaces both decoded models");
			verify(mc);
			previous = mc.player;
			mc.getConnection().sendCommand("pulse_respawn_probe");
			phase = 5;
		}
	}

	private static void verify(Minecraft mc)
	{
		var player = mc.player;
		var position = player.position();
		var velocity = player.getDeltaMovement();
		var bounds = player.getBoundingBox();
		for (int index = 0; index < IDS.length; index++)
		{
			check(INSTANCES[index].player() == player, "native tick refreshes owner before probe lookup");
			var instance = PPlayerAnimations.getInstance(player, IDS[index]);
			var handle = PPlayerAnimations.getHandle(player, IDS[index]);
			check(instance == INSTANCES[index] && instance.player() == player, "UUID instance refreshes owner without losing playback");
			check(instance.definition() == PPlayerAnimations.get(IDS[index]), "registered definition retained");
			check(instance.definition().appliesTo(player, PPlayerPart.ROOT, 0) == (index == 0), "definition mask retained");
			check(handle.controller("motion") == CONTROLLERS[index] && handle.isPaused("motion"), "controller identity/paused state retained");
			check(Math.abs(handle.controller("motion").getInterpolatedTime(1) - (3 + 4 * index)) < 0.00001f, "independent playback times retained");
			var frame = instance.sampleFrame(1);
			check(frame != null && frame != FRAMES[index] && frame.localTransform("root").translation().distance(new Vector3f(1, 0, 0)) < 0.00001f, "real nonzero decoded root frame survives respawn/reload");
			FRAMES[index] = frame;
		}
		check(player.position().equals(position) && player.getDeltaMovement().equals(velocity) && player.getBoundingBox().equals(bounds), "animation sampling preserves physics");
		PLibDatabase.LOGGER.info("STAGE525B SUITE PASS: suite={}, phase={}, ownerId={}", ++suites, phase, player.getId());
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError("STAGE525B: " + message);
	}
}
