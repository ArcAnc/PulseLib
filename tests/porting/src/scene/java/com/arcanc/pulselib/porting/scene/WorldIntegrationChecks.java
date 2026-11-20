/**
 * @author ArcAnc
 * Created at: 20.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.content.renderer.plan.PRenderPlan;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.arcanc.pulselib.porting.scene.OwnerIntegrationChecks.BLOCK_ALPHA;
import static com.arcanc.pulselib.porting.scene.OwnerIntegrationChecks.BLOCK_OPAQUE;
import static com.arcanc.pulselib.porting.scene.OwnerIntegrationChecks.ENTITY_ALPHA;
import static com.arcanc.pulselib.porting.scene.OwnerIntegrationChecks.ENTITY_OPAQUE;
import static com.arcanc.pulselib.porting.scene.OwnerIntegrationChecks.GROUND_OPAQUE;
import static com.arcanc.pulselib.porting.scene.OwnerIntegrationChecks.GUI_ENTITY;

public final class WorldIntegrationChecks
{
	private static final int HAND = 0xFF1050E1;
	private static final List<Integer> colors = new ArrayList<>();
	private static boolean running;
	private static boolean emptyAccepted;
	private static boolean separateTargets;
	private static boolean ownersRendered;
	private static boolean abortedHand;
	private static boolean abortedThisFrame;
	private static boolean completedHand;
	private static boolean guiEntityDrawn;
	private static boolean ownersRemoved;
	private static boolean emptyAfterOwners;
	private static boolean handOitResolved;
	private static int stage;
	private static int removalFrames;
	private static OwnerIntegrationChecks.@Nullable EntityOwner entity;
	private static @Nullable BlockPos chestPos;

	private WorldIntegrationChecks()
	{
	}

	static void start(Minecraft client)
	{
		SceneChecks.check(Files.isRegularFile(Path.of("mods/pulselib-client-scene.jar")), "Only a fresh scene client may create a fixture world");
		client.options.improvedTransparency().set(false);
		client.options.pauseOnLostFocus = false;
		client.options.renderDistance().set(3);
		client.options.simulationDistance().set(5);
		client.gui.setScreen(null);
		running = true;
		client.schedule(() -> client.createWorldOpenFlows().createFreshLevel("pulselib-stage-six",
				new LevelSettings("PulseLib integration", GameType.CREATIVE,
						new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
				new WorldOptions(517, false, false),
				registry -> registry.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen()));
	}

	static boolean tick(Minecraft client)
	{
		if (client.level == null || client.player == null || client.gui.screen() != null)
			return guiEntityDrawn;
		client.player.setYRot(0);
		client.player.setXRot(0);
		if (emptyAccepted && entity == null)
		{
			var player = client.player;
			var owner = new OwnerIntegrationChecks.EntityOwner(client.level, player.getX() - 0.3, player.getY() + 1.5, player.getZ() + 3);
			owner.setId(1_000_000);
			client.level.addEntity(owner);
			entity = owner;
			var pos = BlockPos.containing(player.getX() + 0.3, player.getY() + 1.5, player.getZ() + 6);
			var chest = Blocks.CHEST.defaultBlockState();
			client.level.setBlock(pos, chest, Block.UPDATE_CLIENTS);
			client.level.setBlockEntity(new OwnerIntegrationChecks.BlockOwner(pos, chest));
			chestPos = pos;
		}
		return completedHand;
	}

	static EntityRenderState extractGuiEntity(Minecraft client)
	{
		return client.getEntityRenderDispatcher().extractEntity(Objects.requireNonNull(entity), 0);
	}

	static boolean finish(Minecraft client)
	{
		if (guiEntityDrawn && !ownersRemoved)
		{
			var level = Objects.requireNonNull(client.level);
			level.removeEntity(Objects.requireNonNull(entity).getId(), Entity.RemovalReason.DISCARDED);
			level.setBlock(Objects.requireNonNull(chestPos), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
			entity = null;
			ownersRemoved = true;
			client.gui.setScreen(null);
		}
		return emptyAfterOwners;
	}

	public static void beginFrame()
	{
		if (!running)
			return;
		stage = 0;
		separateTargets = false;
		ownersRendered = false;
		handOitResolved = false;
		abortedThisFrame = false;
		colors.clear();
	}

	public static void targets(LevelTargetBundle targets)
	{
		if (running)
			separateTargets = targets.translucent != null && targets.itemEntity != null && targets.particles != null &&
					targets.weather != null && targets.clouds != null;
	}

	public static void recordPlan(PRenderPlan plan)
	{
		if (!running)
			return;
		for (var group : plan.groups())
			for (var instance : group.instances())
				if (instance.packedColor() == GUI_ENTITY)
				{
					SceneChecks.check(PRenderContexts.current() == PRenderContexts.Context.GUI, "Native GUI entity drained a world plan");
					guiEntityDrawn = true;
				}
				else
					colors.add(instance.packedColor());
	}

	public static void afterOpaque()
	{
		if (!running)
			return;
		SceneChecks.check(stage == 0, "Duplicate/out-of-order opaque world callback");
		stage = 1;
		ownersRendered = colors.containsAll(List.of(ENTITY_OPAQUE, BLOCK_OPAQUE));
		if (ownersRendered)
			SceneChecks.check(colors.contains(GROUND_OPAQUE), "Opaque ground item missed the early depth stage");
		SceneChecks.check(!colors.contains(ENTITY_ALPHA) && !colors.contains(BLOCK_ALPHA), "Ordinary alpha leaked into opaque phase");
	}

	public static void afterNative()
	{
		if (!running)
			return;
		SceneChecks.check(stage == 1, "Depth capture must follow both native and Pulse opaque draws");
		stage = 2;
	}

	public static void afterParticles()
	{
		if (!running)
			return;
		SceneChecks.check(stage == 2, "World alpha must follow depth capture");
		stage = 3;
		if (ownersRendered)
		{
			SceneChecks.check(separateTargets, "Pulse owners did not retain the five native targets");
			SceneChecks.check(colors.indexOf(BLOCK_ALPHA) >= 0 && colors.indexOf(ENTITY_ALPHA) > colors.indexOf(BLOCK_ALPHA),
					"Far block alpha must precede near entity alpha: " + colors);
		}
	}

	public static void hand(PoseStack poses, SubmitNodeCollector collector)
	{
		if (!running || !ownersRendered)
			return;
		collector.submitCustomGeometry(poses, RenderTypes.debugTriangleFan(), (pose, vertices) ->
		{
			SceneChecks.check(stage == 3 && PRenderContexts.current() == PRenderContexts.Context.FIRST_PERSON,
					"Deferred hand feature ran outside its own stage");
			PRenderQueue.submitFirstPersonMesh(PRenderTypes.RenderTypeProvider.trianglesInstantEmissiveSolid(IntegrationChecks.TEXTURE),
					IntegrationChecks.GEOMETRY,
					new PInstanceHeader(new Matrix4f().translation(0, 0, -2).scale(0.15f), HAND, 0xF000F0, OverlayTexture.NO_OVERLAY));
			PRenderQueue.submit(PRenderQueue.RenderStage.FIRST_PERSON, PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(IntegrationChecks.TEXTURE),
					IntegrationChecks.GEOMETRY,
					new PInstanceHeader(new Matrix4f().translation(0.2f, 0, -2).scale(0.1f), 0x8030B090, 0xF000F0, OverlayTexture.NO_OVERLAY),
					PTransparencyMode.OIT);
			if (!abortedHand)
				throw new ExpectedHandAbort();
		});
	}

	public static void handComposite(boolean previouslyPending, boolean stillPending)
	{
		if (running && ownersRendered && PRenderContexts.current() == PRenderContexts.Context.FIRST_PERSON)
		{
			SceneChecks.check(abortedHand && previouslyPending && !stillPending, "First-person OIT was not consumed on its own frame");
			handOitResolved = true;
		}
	}

	public static void recoverHand(Matrix4f matrix)
	{
		SceneChecks.check(running && ownersRendered && !abortedHand && !colors.contains(HAND), "Aborted hand drew a partial frame");
		SceneChecks.check(PRenderContexts.current() == PRenderContexts.Context.WORLD && RenderSystem.getModelViewStack().equals(matrix, 1e-6f),
				"Hand failure leaked its context or model-view stack");
		abortedHand = true;
		abortedThisFrame = true;
	}

	public static void endFrame()
	{
		if (!running)
			return;
		SceneChecks.check(stage == 3, "A real world frame skipped stage callbacks");
		if (entity == null)
		{
			boolean empty = !separateTargets && colors.isEmpty();
			// A native section mesh may still reference the removed block entity until its rebuild is installed.
			if (ownersRemoved && !empty && ++removalFrames < 120)
				return;
			SceneChecks.check(empty, "Empty world frame retained targets=" + separateTargets + " or drew " + colors);
			emptyAccepted = true;
			emptyAfterOwners = ownersRemoved;
			if (emptyAfterOwners)
			{
				running = false;
				System.out.println("PASS world eligibility reset after owner removal, section rebuild frames=" + removalFrames);
			}
		}
		if (ownersRendered && abortedHand && !abortedThisFrame)
		{
			SceneChecks.check(colors.stream().filter(color -> color == HAND).count() == 1,
					"Deferred hand did not draw on the same recovered frame: " + colors);
			SceneChecks.check(handOitResolved, "Deferred first-person OIT did not resolve");
			if (!completedHand)
				System.out.println("PASS actual world stage order with five targets, plan colors=" + colors + ", first-person OIT resolved after deferred feature");
			completedHand = true;
		}
	}

	public static final class ExpectedHandAbort extends RuntimeException
	{
	}
}
