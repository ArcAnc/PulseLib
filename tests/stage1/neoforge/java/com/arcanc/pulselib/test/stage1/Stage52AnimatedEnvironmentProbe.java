/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.registration.PAnimatedAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.baked.PMeshRenderContext;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.model.baked.PMeshRenderMaterial;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchor;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentContext;
import com.arcanc.pulselib.content.player.animation.attachment.PPlayerAnimatedAttachmentRenderer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.PResourceCache;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Independent clear/terrain controls for animated native armor and explicit ordinary/OIT sockets. */
public final class Stage52AnimatedEnvironmentProbe
{
	private static final int PLAYER = 0x526b01;
	private static final List<PModelData> RIGS = List.of(
			PModelData.direct(PLibDatabase.rl("player/regression/depth_motion"), PGeckoModelLoader.INSTANCE.id()),
			PModelData.direct(PLibDatabase.rl("player/regression/depth_motion_gltf"), PGltfModelLoader.INSTANCE.id()));
	private static final List<Identifier> IDS = List.of(PLibDatabase.rl("stage52b4c2b1/gecko"), PLibDatabase.rl("stage52b4c2b1/gltf"));
	private static final PPlayerAnimationAnchor ANCHOR = new PPlayerAnimationAnchor(PLibDatabase.rl("stage52b4c2b1/body"));
	private static final String[] KINDS = {"native-armor", "custom-armor", "anchor-opaque", "anchor-ordinary", "anchor-oit"};
	private static final String[] STATES = {"clear-background", "clear-front", "terrain-background", "terrain-front", "clear-background-behind", "clear-behind", "terrain-background-behind", "terrain-behind"};
	private static final List<Case> CASES = new ArrayList<>();
	private static final Map<Model<?>, Model<?>> CUSTOM = new IdentityHashMap<>();
	private static final int[][] CALLBACKS = new int[6][40];
	private static final int[][] SAMPLES = new int[9][2];
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static final Matrix4f VIEW = new Matrix4f();
	private static List<float[]> early = List.of();
	private static List<float[]> terrain = List.of();
	private static List<float[]> nativeItem = List.of();
	private static List<float[]> beforeResolve = List.of();
	private static boolean started;
	private static boolean completed;
	private static boolean advance;
	private static boolean armed;
	private static int phase;
	private static int group;
	private static int state;
	private static int configured = -1;
	private static int startTick;
	private static int frames;
	private static int comparisons;
	private static int motion;
	private static int formats;
	private static int green;
	private static int selections;
	private static long deadline;
	private static GraphicsPreset originalPreset = GraphicsPreset.CUSTOM;
	private static CameraType originalCamera = CameraType.FIRST_PERSON;
	private static boolean originalGui;
	private static boolean originalBob;
	private static @Nullable CompletableFuture<Void> reload;

	private Stage52AnimatedEnvironmentProbe()
	{
	}

	public static void register(IEventBus bus)
	{
		bus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(new IClientItemExtensions()
		{
			@Override
			public Model<?> getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original)
			{
				if (!started || completed || kind() != 1)
					return original;
				selections++;
				return CUSTOM.computeIfAbsent(original, ignored -> Stage52ArmorOcclusionProbe.custom());
			}
		}, Items.DIAMOND_CHESTPLATE));
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52AnimatedEnvironmentProbe::early);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52AnimatedEnvironmentProbe::nativeLayers);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52AnimatedEnvironmentProbe::finish);
		NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) ->
		{
			check(completed && CASES.size() == 1920 && comparisons == 10800 && motion == 2160 && formats == 2160 && green == 2592 && selections > 0,
					"All animated terrain groups and discriminating pixel/depth witnesses must finish");
			PLibDatabase.LOGGER.info("ANIMATED ENVIRONMENT SHUTDOWN PASS");
		});
	}

	public static void registerModels(PModelRegistrar registrar)
	{
		for (var rig : RIGS)
			registrar.model(rig).texture("0", Stage42aWorldStageProbe.WHITE);
		registrar.model(Stage52PlayerOcclusionProbe.MODEL).texture("0", Stage42aWorldStageProbe.WHITE);
	}

	public static void registerAnimations(PPlayerAnimationRegistrar registrar)
	{
		for (int format = 0; format < 2; format++)
		{
			int selected = format;
			registrar.register(IDS.get(format), PPlayerAnimationDefinition.builder(RIGS.get(format)).
					when(player -> started && !completed && player.getId() == PLAYER && format() == selected).
					bind(PPlayerPart.BODY, "body").anchor(ANCHOR, "body").meshAttachment("no_automatic_mesh").
					crossfade(0, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
					controllers(controllers -> controllers.add("depth", () -> value -> value.controller().getState())).build());
		}
	}

	public static void registerAttachments(PAnimatedAttachmentRegistrar registrar)
	{
		registrar.register(new PPlayerAnimatedAttachmentRenderer()
		{
			@Override
			public PPlayerAnimationAnchor anchor()
			{
				return ANCHOR;
			}

			@Override
			public void render(PPlayerAnimatedAttachmentContext context)
			{
				if (!started || completed || kind() < 2)
					return;
				check(!context.firstPerson() && context.player().getId() == PLAYER && context.animation().equals(IDS.get(format())), "Active third-person owner submits its socket");
				var poses = context.poseStack();
				poses.pushPose();
				try
				{
					poses.translate(0, 0.15, -0.4);
					poses.scale(0.7f, 0.4f, 0.15f);
					var mesh = Stage52PlayerOcclusionProbe.MODEL.getModel().bones().getFirst().meshes().getFirst();
					var inputs = new PMeshRenderContext(PRenderTypes.RenderTypeProvider::trianglesSolid,
							kind() == 2 ? 0xFF00FF00 : 0x8000FF00, context.packedLight(), OverlayTexture.NO_OVERLAY).
							withEmissive(true).withAlphaMode(kind() == 2 ? PAlphaMode.OPAQUE : PAlphaMode.TRANSLUCENT);
					var material = PMeshRenderMaterial.resolve(mesh, inputs);
					var type = material.resolveRenderType(inputs, PResourceCache.ATLAS_LOCATION);
					check(PRenderTypes.isTransparent(type) == (kind() >= 3) && PRenderTypes.usesOit(type) == (kind() >= 3), "Same canonical material type supports the explicit policy");
					PRenderQueue.submitEntityMesh(type, material.mesh(), null,
							new PRenderQueue.InstanceData(new Matrix4f(poses.last().pose()), inputs.color(), material.packedLight(), inputs.packedOverlay()),
							kind() == 4 ? PTransparencyMode.OIT : PTransparencyMode.ORDINARY);
					CALLBACKS[phase][group]++;
				}
				finally
				{
					poses.popPose();
				}
			}
		});
	}

	private static int format()
	{
		return group / 20;
	}

	private static int kind()
	{
		return group % 20 / 4;
	}

	private static int pose()
	{
		return group % 4 / 2;
	}

	private static int environment()
	{
		return group % 2;
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.getOverlay() != null || RIGS.stream().anyMatch(rig -> rig.getModel() == null))
			return;
		if (!started)
		{
			check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-animated-environment")), "Disposable animated environment client marker exists");
			for (var rig : RIGS)
			{
				var clip = rig.getModel().animations().get("depth");
				check(clip != null && clip.length() == 20 && clip.boneAnimations().containsKey("body"), "Real decoded body channel exists");
			}
			originalPreset = mc.options.graphicsPreset().get();
			originalCamera = mc.options.getCameraType();
			originalGui = mc.options.hideGui;
			originalBob = mc.options.bobView().get();
			mc.options.hideGui = true;
			mc.options.bobView().set(false);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			var owner = new RemotePlayer(mc.level, new GameProfile(new UUID(0, PLAYER), "AnimatedTerrain"));
			owner.setId(PLAYER);
			mc.level.addEntity(owner);
			started = true;
			deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(15);
			mode();
		}
		check(System.nanoTime() < deadline, "Animated environment timed out phase=" + phase + " group=" + group + " state=" + state);
		if (reload != null)
		{
			if (!reload.isDone())
				return;
			reload.join();
			reload = null;
			mode();
		}
		if (advance)
		{
			advance = false;
			if (++state == 8)
			{
				state = 0;
				if (++group == 40)
				{
					group = 0;
					if (++phase == 6)
					{
						complete();
						return;
					}
					if (phase == 3)
					{
						reload = mc.reloadResourcePacks();
						return;
					}
					mode();
				}
			}
			frames = 0;
			startTick = Math.max(startTick, mc.player.tickCount);
		}
		KeyMapping.releaseAll();
		mc.mouseHandler.releaseMouse();
		double x = state == 0 || state == 1 || state == 4 || state == 5 ? 12.5 : environment() == 0 ? -3.5 : 4.5;
		place(mc.player, x, -6, 0);
		var owner = (RemotePlayer)mc.level.getEntity(PLAYER);
		check(owner != null, "Real native player exists");
		if (configured != phase * 40 + group)
		{
			configured = phase * 40 + group;
			for (var id : IDS)
				PPlayerAnimations.getHandle(owner, id).stopAll();
			owner.setItemSlot(EquipmentSlot.CHEST, kind() < 2 ? new ItemStack(Items.DIAMOND_CHESTPLATE) : ItemStack.EMPTY);
			check(PPlayerAnimations.getHandle(owner, IDS.get(format())).play("depth", PRawAnimation.begin().thenLoop("depth").build()), "Real manager starts decoded body animation");
		}
		var controller = PPlayerAnimations.getHandle(owner, IDS.get(format())).controller("depth");
		check(controller != null, "Real controller exists");
		controller.seek(RIGS.get(format()).getModel(), pose() * 10);
		controller.pause();
		check(controller.getState() == ControllerState.PAUSE && controller.getInterpolatedTime(0.5f) == pose() * 10, "Hold exact native pose between independent control frames");
		place(owner, state % 2 == 0 ? x - 2 : x, state >= 4 ? 5 : 0, 180);
	}

	private static void place(Entity entity, double x, double z, float yaw)
	{
		entity.setPos(x, 80, z);
		entity.xo = entity.xOld = x;
		entity.yo = entity.yOld = 80;
		entity.zo = entity.zOld = z;
		entity.setYRot(yaw);
		entity.yRotO = yaw;
		entity.setXRot(0);
		entity.xRotO = 0;
		entity.setDeltaMovement(0, 0, 0);
		if (entity instanceof RemotePlayer owner)
			owner.yBodyRot = owner.yBodyRotO = owner.yHeadRot = owner.yHeadRotO = yaw;
	}

	private static void mode()
	{
		var mc = Minecraft.getInstance();
		mc.options.graphicsPreset().set(GraphicsPreset.values()[phase % 3]);
		mc.levelRenderer.allChanged();
		frames = 0;
		startTick = mc.player.tickCount + 40;
		armed = false;
	}

	private static void early(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		var mc = Minecraft.getInstance();
		if (!started || completed || advance || reload != null || mc.getOverlay() != null || mc.player.tickCount < startTick + 2 || ++frames < 4)
			return;
		double x = mc.player.getX();
		if (!mc.levelRenderer.isSectionCompiledAndVisible(BlockPos.containing(x, 81, 1)) || !mc.levelRenderer.isSectionCompiledAndVisible(BlockPos.containing(x, 81, 8)))
			return;
		check(Minecraft.useShaderTransparency() == (phase % 3 == 2), "Native graphics option matches requested mode");
		var owner = event.getLevelRenderState().entityRenderStates.stream().filter(value -> value instanceof AvatarRenderState avatar && avatar.id == PLAYER).findFirst().orElse(null);
		if (owner == null)
			return;
		check(((PWorldRenderState)owner).pulselib$requiresSeparateTargets(), "Real extracted rig/anchor retains native terrain targets even for offset controls");
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		var frame = new PGlRenderHost().captureFrame();
		PROJECTION.set(frame.projection());
		VIEW.set(frame.modelView());
		var vp = new Matrix4f(PROJECTION).mul(VIEW);
		var target = mc.getMainRenderTarget();
		for (int sample = 0; sample < 9; sample++)
		{
			var screen = vp.transform(new Vector4f((float)(x + (sample % 3 - 1) * 0.08 - camera.x),
					(float)(81.05 + (sample / 3 - 1) * 0.12 - camera.y), (float)((state >= 4 ? 5 : 0) - camera.z), 1));
			SAMPLES[sample][0] = (int)((screen.x / screen.w + 1) * 0.5f * target.width);
			SAMPLES[sample][1] = (int)((screen.y / screen.w + 1) * 0.5f * target.height);
			check(SAMPLES[sample][0] > 0 && SAMPLES[sample][0] < target.width && SAMPLES[sample][1] > 0 && SAMPLES[sample][1] < target.height, "Torso sample is in viewport");
		}
		early = pixels(target);
		armed = true;
	}

	private static void nativeLayers(RenderLevelStageEvent.AfterWeather event)
	{
		if (!armed || advance)
			return;
		var mc = Minecraft.getInstance();
		var terrainTarget = mc.levelRenderer.getTranslucentTarget();
		var itemTarget = mc.levelRenderer.getItemEntityTarget();
		check(terrainTarget != null && itemTarget != null, "Native terrain and item targets survive to actual composition");
		terrain = pixels(terrainTarget);
		nativeItem = pixels(itemTarget);
		beforeResolve = pixels(mc.getMainRenderTarget());
	}

	private static void finish(RenderLevelStageEvent.AfterLevel event)
	{
		if (!armed || advance)
			return;
		var mc = Minecraft.getInstance();
		var result = pixels(mc.getMainRenderTarget());
		List<Pixel> pixels = new ArrayList<>();
		for (int sample = 0; sample < 9; sample++)
			pixels.add(new Pixel(SAMPLES[sample][0], SAMPLES[sample][1], early.get(sample), terrain.get(sample), nativeItem.get(sample), beforeResolve.get(sample), result.get(sample), distance(Math.min(result.get(sample)[4], nativeItem.get(sample)[4]))));
		CASES.add(new Case(phase, group, state, KINDS[kind()], format() == 0 ? "gecko" : "gltf", pose() * 10,
				environment() == 0 ? "glass" : "contained-water", STATES[state], mc.options.graphicsPreset().get().toString(), Minecraft.useShaderTransparency(),
				PROJECTION.get(new float[16]), VIEW.get(new float[16]), pixels));
		if (state == 7)
		{
			writeReport();
			compare();
			if (group % 4 == 0)
				PLibDatabase.LOGGER.info("ANIMATED ENVIRONMENT GROUP PASS: phase={}, group={}, owner={}, states={}, comparisons={}, motion={}, formats={}, green={}", phase, group, KINDS[kind()], CASES.size(), comparisons, motion, formats, green);
			if (phase == 2 && group == 36)
				Screenshot.grab(mc.gameDirectory, "animated-terrain-oit.png", mc.getMainRenderTarget(), 1,
						message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		}
		armed = false;
		advance = true;
	}

	private static void compare()
	{
		int base = CASES.size() - 8;
		for (int sample = 0; sample < 9; sample++)
		{
			var background = CASES.get(base).pixels.get(sample);
			var front = CASES.get(base + 1).pixels.get(sample);
			var terrainOnly = CASES.get(base + 6).pixels.get(sample);
			var backgroundBehind = CASES.get(base + 4).pixels.get(sample);
			var combinedFront = CASES.get(base + 3).pixels.get(sample);
			var behind = CASES.get(base + 5).pixels.get(sample);
			var combinedBehind = CASES.get(base + 7).pixels.get(sample);
			check(rgb(front.result, background.result) > 0.05f && rgb(behind.result, background.result) > 0.05f, "Both isolated native owner controls cover actual pixels");
			float alpha = terrainOnly.terrain[3];
			check(alpha > 0.05f && alpha < 0.99f && terrainOnly.terrain[4] < 1 && rgb(terrainOnly.result, background.result) > 0.03f, "Real native terrain contributes nonzero alpha/depth/RGB");
			check(front.nearestDistance < distance(terrainOnly.terrain[4]) - 0.5f && behind.nearestDistance > distance(terrainOnly.terrain[4]) + 0.5f, "Actual GPU owner surfaces straddle native terrain");
			float[] expected = new float[3];
			for (int color = 0; color < 3; color++)
				expected[color] = terrainOnly.result[color] + (1 - alpha) * (behind.result[color] - backgroundBehind.result[color]);
			float[] nativeExpected = new float[3];
			for (int color = 0; color < 3; color++)
				nativeExpected[color] = terrainOnly.terrain[color] + (1 - alpha) * backgroundBehind.result[color];
			verify(rgb(terrainOnly.result, nativeExpected) < 0.025f, "Independent native terrain resolve", sample, nativeExpected, terrainOnly.result);
			verify(Math.abs(combinedBehind.nearestDistance - behind.nearestDistance) < 0.0005f, "Behind owner depth", sample, behind.result, combinedBehind.result);
			verify(rgb(combinedFront.result, front.result) < 0.025f, "Front owner RGB", sample, front.result, combinedFront.result);
			verify(Math.abs(combinedFront.nearestDistance - front.nearestDistance) < 0.0005f, "Front owner depth", sample, front.result, combinedFront.result);
			verify(rgb(combinedBehind.result, expected) < 0.035f, "Terrain over behind owner RGB", sample, expected, combinedBehind.result);
			if (pose() == 1)
				for (int control : new int[]{1, 5})
				{
					var previous = CASES.get(base - 16 + control).pixels.get(sample);
					var actual = CASES.get(base + control).pixels.get(sample);
					check(Math.abs(actual.nearestDistance - previous.nearestDistance) > 0.15f, "Decoded animation changes actual isolated depth surface");
					motion++;
				}
			if (format() == 1)
				for (int control : new int[]{1, 5})
				{
					var gecko = CASES.get(base - 160 + control).pixels.get(sample);
					var actual = CASES.get(base + control).pixels.get(sample);
					check(Math.abs(actual.nearestDistance - gecko.nearestDistance) < 0.002f, "Gecko/glTF produce equivalent native depth surfaces");
					formats++;
				}
			if (kind() >= 2)
				for (var control : List.of(front, behind))
				{
					check(control.result[1] > control.result[0] + 0.1f && control.result[1] > control.result[2] + 0.1f, "Real socket geometry emits green pixels");
					green++;
				}
		}
	}

	private static void verify(boolean condition, String message, int sample, float[] expected, float[] actual)
	{
		comparisons++;
		check(condition, message + " phase=" + phase + " group=" + group + " sample=" + sample + " expected=" + java.util.Arrays.toString(expected) + " actual=" + java.util.Arrays.toString(actual));
	}

	private static float rgb(float[] first, float[] second)
	{
		return Math.max(Math.abs(first[0] - second[0]), Math.max(Math.abs(first[1] - second[1]), Math.abs(first[2] - second[2])));
	}

	private static float distance(float depth)
	{
		var view = new Matrix4f(PROJECTION).invert().transform(new Vector4f(0, 0, depth * 2 - 1, 1));
		return -view.z / view.w;
	}

	private static List<float[]> pixels(RenderTarget target)
	{
		List<float[]> values = new ArrayList<>();
		for (int[] sample : SAMPLES)
			values.add(pixel(target, sample[0], sample[1]));
		return values;
	}

	private static float[] pixel(RenderTarget target, int x, int y)
	{
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
			check(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Complete native environment readback framebuffer");
			var rgba = BufferUtils.createFloatBuffer(4);
			var depth = BufferUtils.createFloatBuffer(1);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, rgba);
			GL11.glReadPixels(x, y, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
			return new float[]{rgba.get(0), rgba.get(1), rgba.get(2), rgba.get(3), depth.get(0)};
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void complete()
	{
		for (int mode = 0; mode < 6; mode++)
			for (int value = 0; value < 40; value++)
				if (value % 20 / 4 >= 2)
					check(CALLBACKS[mode][value] > 0, "Every attachment/material/environment/mode group must submit actual geometry");
		completed = true;
		writeReport();
		var mc = Minecraft.getInstance();
		mc.options.graphicsPreset().set(originalPreset);
		mc.options.setCameraType(originalCamera);
		mc.options.hideGui = originalGui;
		mc.options.bobView().set(originalBob);
		CUSTOM.clear();
		if (Boolean.getBoolean("pulselib.stage2Probe"))
			Stage2PluginProbe.verify(true);
		PLibDatabase.LOGGER.info("ANIMATED ENVIRONMENT PASS: states={}, comparisons={}, motion={}, formats={}, green={}, selections={}, callbacks={}", CASES.size(), comparisons, motion, formats, green, selections, java.util.Arrays.deepToString(CALLBACKS));
		mc.stop();
	}

	private static void writeReport()
	{
		try
		{
			Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("animated-environment-results.json"),
					new GsonBuilder().setPrettyPrinting().create().toJson(new Report(completed ? "PASS" : "INCOMPLETE", comparisons, motion, formats, green, selections, CALLBACKS, CASES)));
		}
		catch (IOException exception)
		{
			throw new UncheckedIOException(exception);
		}
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private record Pixel(int x, int y, float[] early, float[] terrain, float[] item, float[] beforeResolve, float[] result, float nearestDistance)
	{
	}

	private record Case(int phase, int group, int control, String owner, String format, int ticks, String terrain, String state, String preset, boolean improved, float[] projection, float[] view, List<Pixel> pixels)
	{
	}

	private record Report(String status, int comparisons, int motion, int formats, int green, int selections, int[][] callbacks, List<Case> cases)
	{
	}
}
