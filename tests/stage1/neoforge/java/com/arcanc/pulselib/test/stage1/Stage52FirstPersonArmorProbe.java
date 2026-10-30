/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.api.registration.PAttachmentRegistrar;
import com.arcanc.pulselib.api.registration.PModelRegistrar;
import com.arcanc.pulselib.api.registration.PPlayerAnimationRegistrar;
import com.arcanc.pulselib.content.model.animation.PPoseEasing;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.model.animation.PTransitionInterruptionPolicy;
import com.arcanc.pulselib.content.model.textures.PAlphaMode;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationAnchors;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimationDefinition;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.player.animation.PPlayerPart;
import com.arcanc.pulselib.content.player.animation.firstPerson.PFirstPersonCameraMode;
import com.arcanc.pulselib.content.player.animation.firstPerson.PPlayerFirstPersonSettings;
import com.arcanc.pulselib.content.renderer.PRenderContexts;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.content.renderer.modelData.PModelData;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.data.gecko.PGeckoModelLoader;
import com.arcanc.pulselib.data.gltf.PGltfModelLoader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.arcanc.pulselib.util.attachments.PLivingAttachmentDefinition;
import com.arcanc.pulselib.util.attachments.PTransform;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidAnchors;
import com.arcanc.pulselib.util.attachments.humanoid.PHumanoidBindings;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.opengl.GlTexture;

import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Native hand geometry and legacy persistent armor, with independent bare/front/behind controls. */
public final class Stage52FirstPersonArmorProbe
{
	private static final List<PModelData> RIGS = List.of(
			PModelData.direct(PLibDatabase.rl("player/regression/first_person_armor"), PGeckoModelLoader.INSTANCE.id()),
			PModelData.direct(PLibDatabase.rl("player/regression/first_person_armor_gltf"), PGltfModelLoader.INSTANCE.id()));
	private static final List<Identifier> IDS = List.of(PLibDatabase.rl("stage52b4c2a/gecko"), PLibDatabase.rl("stage52b4c2a/gltf"));
	private static final List<Case> CASES = new ArrayList<>();
	private static final int[][] SUBMISSIONS = new int[6][36];
	private static final int[][] RESOLVERS = new int[6][36];
	private static final int[][] SAMPLES = new int[18][2];
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static final Matrix4f VIEW = new Matrix4f();
	private static final List<String> FAILURES = new ArrayList<>();
	private static int phase;
	private static int variant;
	private static int configured = -1;
	private static int frames;
	private static int startTick;
	private static int ticks;
	private static int comparisons;
	private static int green;
	private static int motion;
	private static boolean started;
	private static boolean completed;
	private static boolean advance;
	private static boolean armed;
	private static GraphicsPreset originalPreset = GraphicsPreset.CUSTOM;
	private static CameraType originalCamera = CameraType.FIRST_PERSON;
	private static boolean originalGui;
	private static boolean originalBob;
	private static @Nullable CompletableFuture<Void> reload;
	private static List<float[]> before = List.of();
	private static List<float[]> nativeFeatures = List.of();
	private static double[] depthRange = {0, 1};

	private Stage52FirstPersonArmorProbe()
	{
	}

	public static void register()
	{
		NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) ->
		{
			check(completed && CASES.size() == 216 && comparisons == 9072 && green == 1728 && motion == 1944 && FAILURES.isEmpty(), "Entire persistent hand matrix must finish without mismatches");
			PLibDatabase.LOGGER.info("FIRST PERSON ARMOR SHUTDOWN PASS");
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
					when(player -> started && !completed && format() == selected && player == Minecraft.getInstance().player).
					bind(PPlayerPart.RIGHT_ARM, "right_arm").bind(PPlayerPart.LEFT_ARM, "left_arm").
					anchor(PPlayerAnimationAnchors.FIRST_PERSON_CAMERA, "camera").meshAttachment("no_automatic_mesh").
					firstPerson(new PPlayerFirstPersonSettings(true, 0, 0, PFirstPersonCameraMode.VANILLA)).
					crossfade(0, PPoseEasing.LINEAR, PTransitionInterruptionPolicy.FROM_CURRENT).
					controllers(controllers -> controllers.add("depth", () -> state -> state.controller().getState())).build());
		}
	}

	public static void registerAttachments(PAttachmentRegistrar registrar)
	{
		for (int source = 0; source < 2; source++)
			for (int placement = 0; placement < 2; placement++)
			{
				int selected = source, position = placement;
				var definition = new PLivingAttachmentDefinition(Stage52PlayerOcclusionProbe.MODEL,
						(entity, slot, stack) -> started && !completed && entity == Minecraft.getInstance().player && kind() > 0 &&
								source() == selected && placement() == position && (selected == 0 || slot == EquipmentSlot.CHEST),
						List.of(PHumanoidBindings.bind(PHumanoidAnchors.RIGHT_ARM, "probe", transform(1, placement)),
								PHumanoidBindings.bind(PHumanoidAnchors.LEFT_ARM, "probe", transform(-1, placement))),
						(entity, stack, bone, mesh, inherited, tick) ->
						{
							check(selected == 0 ? stack.isEmpty() : stack.is(Items.DIAMOND_CHESTPLATE), "Legacy source stack belongs to the selected definition");
							RESOLVERS[phase][variant]++;
							return inherited.withColor(alpha() ? 0x8000FF00 : 0xFF00FF00).withEmissive(true).
									withAlphaMode(alpha() ? PAlphaMode.TRANSLUCENT : PAlphaMode.OPAQUE);
						}, source == 1);
				if (source == 0)
					registrar.registerGlobalLiving(definition);
				else
					registrar.registerLiving(Items.DIAMOND_CHESTPLATE, definition);
			}
	}

	private static PTransform transform(int side, int placement)
	{
		return new PTransform(new Vector3f(side * 0.0625f, -0.4f, placement == 0 ? 0.25f : -0.3f), new Quaternionf(), new Vector3f(0.36f, 0.36f, 0.15f));
	}

	private static int format()
	{
		return variant / 18;
	}

	private static int kind()
	{
		return variant % 18 / 2;
	}

	private static int source()
	{
		return (kind() - 1) % 4 / 2;
	}

	private static int placement()
	{
		return (kind() - 1) / 4;
	}

	private static boolean alpha()
	{
		return kind() > 0 && kind() % 2 == 0;
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.getOverlay() != null || RIGS.stream().anyMatch(model -> model.getModel() == null))
			return;
		check(++ticks < 12000, "Persistent hand matrix timed out");
		if (!started)
		{
			check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-first-person-armor")), "Disposable client marker exists");
			originalPreset = mc.options.graphicsPreset().get();
			originalCamera = mc.options.getCameraType();
			originalGui = mc.options.hideGui;
			originalBob = mc.options.bobView().get();
			mc.options.hideGui = false;
			mc.options.bobView().set(false);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			started = true;
			mode();
		}
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
			variant++;
			frames = 0;
			startTick = mc.player.tickCount;
			if (variant == 36)
			{
				variant = 0;
				configured = -1;
				phase++;
				if (phase == 6)
				{
					completed = true;
					write();
					check(FAILURES.isEmpty(), "Persistent hand pixels failed: " + FAILURES);
					for (int mode = 0; mode < 6; mode++)
						for (int owner = 0; owner < 36; owner++)
							if (owner % 18 / 2 > 0)
								check(SUBMISSIONS[mode][owner] > 0 && SUBMISSIONS[mode][owner] == RESOLVERS[mode][owner], "Every persistent mesh must use the real explicit ordinary hand queue");
					mc.options.graphicsPreset().set(originalPreset);
					mc.options.setCameraType(originalCamera);
					mc.options.hideGui = originalGui;
					mc.options.bobView().set(originalBob);
					Stage2PluginProbe.verify(true);
					PLibDatabase.LOGGER.info("FIRST PERSON ARMOR PASS: states={}, comparisons={}, green={}, motion={}, resolvers={}", CASES.size(), comparisons, green, motion, java.util.Arrays.deepToString(RESOLVERS));
					mc.stop();
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
		mc.mouseHandler.releaseMouse();
		KeyMapping.releaseAll();
		mc.player.setPos(0, 80, -6);
		mc.player.xo = mc.player.xOld = 0;
		mc.player.yo = mc.player.yOld = 80;
		mc.player.zo = mc.player.zOld = -6;
		mc.player.setYRot(0);
		mc.player.yRotO = 0;
		mc.player.setXRot(0);
		mc.player.xRotO = 0;
		mc.player.setDeltaMovement(0, 0, 0);
		mc.player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		mc.player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		if (configured != variant)
		{
			configured = variant;
			for (var id : IDS)
				PPlayerAnimations.getHandle(mc.player, id).stopAll();
			mc.player.setItemSlot(EquipmentSlot.CHEST, kind() > 0 && source() == 1 ? new ItemStack(Items.DIAMOND_CHESTPLATE) : ItemStack.EMPTY);
			check(PPlayerAnimations.getHandle(mc.player, IDS.get(format())).play("depth", PRawAnimation.begin().thenLoop("depth").build()), "Start decoded first-person arm clip");
		}
		var model = RIGS.get(format()).getModel();
		check(model.animations().get("depth").length() == 20, "Real decoded motion channel has the expected duration");
		var controller = PPlayerAnimations.getHandle(mc.player, IDS.get(format())).controller("depth");
		check(controller != null, "Real controller exists");
		controller.seek(model, variant % 2 * 10);
		controller.pause();
	}

	private static void mode()
	{
		var mc = Minecraft.getInstance();
		mc.options.graphicsPreset().set(GraphicsPreset.values()[phase % 3]);
		mc.levelRenderer.allChanged();
		frames = 0;
		startTick = mc.player.tickCount + 40;
	}

	public static void beforeNative()
	{
		var mc = Minecraft.getInstance();
		if (!started || completed || advance || reload != null || mc.getOverlay() != null || mc.player.tickCount < startTick + 5 || ++frames < 8)
			return;
		var frame = new PGlRenderHost().captureFrame();
		PROJECTION.set(frame.projection());
		VIEW.set(frame.modelView());
		var range = BufferUtils.createDoubleBuffer(2);
		GL11.glGetDoublev(GL11.GL_DEPTH_RANGE, range);
		depthRange = new double[] {range.get(0), range.get(1)};
		var vp = new Matrix4f(PROJECTION).mul(VIEW);
		var target = mc.getMainRenderTarget();
		for (int sample = 0; sample < 18; sample++)
		{
			int side = sample / 9 == 0 ? 1 : -1, point = sample % 9;
			var v = vp.transform(new Vector4f(side * 0.2875f + (point % 3 - 1) * 0.015f, 0.15f + (point / 3 - 1) * 0.03f, -1.2f - variant % 2 * 0.2f, 1));
			int x = (int)((v.x / v.w + 1) * 0.5f * target.width), y = (int)((v.y / v.w + 1) * 0.5f * target.height);
			check(x > 0 && y > 0 && x < target.width && y < target.height, "Authored hand sample remains inside viewport");
			SAMPLES[sample][0] = x;
			SAMPLES[sample][1] = y;
		}
		before = pixels();
		armed = true;
	}

	public static void afterNative()
	{
		if (armed)
			nativeFeatures = pixels();
	}

	public static void afterFlush()
	{
		if (!armed)
			return;
		var pixels = pixels();
		var distances = pixels.stream().map(value -> distance(value[4])).toList();
		CASES.add(new Case(phase, variant, java.util.Arrays.stream(SAMPLES).map(int[]::clone).toArray(int[][]::new), PROJECTION.get(new float[16]), VIEW.get(new float[16]), depthRange, before, nativeFeatures, pixels, distances));
		if (phase == 0 && variant < 6)
			Screenshot.grab(Minecraft.getInstance().gameDirectory, "first-person-armor-" + variant + ".png", Minecraft.getInstance().getMainRenderTarget(), 1, message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		write();
		verify(pixels, distances);
		write();
		armed = false;
		advance = true;
	}

	private static void verify(List<float[]> pixels, List<Float> distances)
	{
		var baseline = CASES.get(phase * 36 + format() * 18 + variant % 2);
		for (int sample = 0; sample < 18; sample++)
		{
			float[] actual = pixels.get(sample), bare = baseline.afterFlush.get(sample);
			check(distances.get(sample) > 0 && distances.get(sample) < 3, "Native hand occupies authored sample: " + variant + "/" + sample + " pixel=" + java.util.Arrays.toString(actual) + " distance=" + distances.get(sample));
			if (kind() > 0)
			{
				if (placement() == 0)
				{
					check(alpha() ? actual[1] > bare[1] + 0.08f && actual[0] < bare[0] - 0.02f : actual[1] > 0.95f && actual[0] < 0.02f && actual[2] < 0.02f,
							"Persistent armor must emit green pixels: variant=" + variant + " sample=" + sample + " actual=" + java.util.Arrays.toString(actual) + " bare=" + java.util.Arrays.toString(bare));
					green++;
					if (!alpha())
						require(baseline.distances.get(sample) - distances.get(sample) > 0.08f, "Front opaque armor writes nearer depth");
				}
				else
					require(rgb(actual, bare) < 0.025f, "Behind armor must be hidden by native hand");
				if (alpha() || placement() == 1)
					require(Math.abs(distances.get(sample) - baseline.distances.get(sample)) < 0.0005f, "Ordinary alpha/hidden armor preserves native depth");
			}
			if (variant % 2 == 1)
			{
				var previous = CASES.get(CASES.size() - 2);
				require(Math.abs(distances.get(sample) - previous.distances.get(sample)) > 0.1f, "Decoded arm motion changes actual depth");
				motion++;
			}
			if (format() == 1)
			{
				var gecko = CASES.get(phase * 36 + variant - 18);
				require(Math.abs(distances.get(sample) - gecko.distances.get(sample)) < 0.002f, "Equivalent Gecko/glTF first-person depth");
			}
		}
		PLibDatabase.LOGGER.info("FIRST PERSON ARMOR MATRIX: phase={}, variant={}, states={}, comparisons={}, errors={}", phase, variant, CASES.size(), comparisons, FAILURES.size());
	}

	public static void submitted(RenderType type, PTransparencyMode transparency)
	{
		check(PRenderContexts.current() == PRenderContexts.Context.FIRST_PERSON && transparency == PTransparencyMode.ORDINARY,
				"Persistent armor retains the native first-person context and explicit ordinary blending");
		check(PRenderTypes.isTransparent(type) == alpha(), "Actual submitted material matches the requested alpha mode");
		SUBMISSIONS[phase][variant]++;
	}

	private static float rgb(float[] a, float[] b)
	{
		return Math.max(Math.abs(a[0] - b[0]), Math.max(Math.abs(a[1] - b[1]), Math.abs(a[2] - b[2])));
	}

	private static float distance(float depth)
	{
		float normalized = (float)((depth - depthRange[0]) / (depthRange[1] - depthRange[0]));
		var value = new Matrix4f(PROJECTION).invert().transform(new Vector4f(0, 0, normalized * 2 - 1, 1));
		return -value.z / value.w;
	}

	private static List<float[]> pixels()
	{
		var target = Minecraft.getInstance().getMainRenderTarget();
		int previous = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING), fbo = GL30.glGenFramebuffers();
		try
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
			GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, ((GlTexture)target.getDepthTexture()).glId(), 0);
			check(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE, "Complete hand readback framebuffer");
			GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
			List<float[]> result = new ArrayList<>();
			for (int[] sample : SAMPLES)
			{
				var rgba = BufferUtils.createFloatBuffer(4);
				var depth = BufferUtils.createFloatBuffer(1);
				GL11.glReadPixels(sample[0], sample[1], 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, rgba);
				GL11.glReadPixels(sample[0], sample[1], 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
				result.add(new float[] {rgba.get(0), rgba.get(1), rgba.get(2), rgba.get(3), depth.get(0)});
			}
			return result;
		}
		finally
		{
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previous);
			GL30.glDeleteFramebuffers(fbo);
		}
	}

	private static void require(boolean condition, String message)
	{
		comparisons++;
		if (!condition)
			FAILURES.add("phase=" + phase + " variant=" + variant + " " + message);
	}

	private static void check(boolean condition, String message)
	{
		if (!condition)
			throw new AssertionError(message);
	}

	private static void write()
	{
		try
		{
			Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("first-person-armor-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
					new Report(completed ? FAILURES.isEmpty() ? "PASS" : "FAIL" : "INCOMPLETE", comparisons, green, motion, CASES, FAILURES)));
		}
		catch (IOException exception)
		{
			throw new UncheckedIOException(exception);
		}
	}

	private record Case(int phase, int variant, int[][] samples, float[] projection, float[] view, double[] depthRange, List<float[]> beforeFeatures,
			List<float[]> afterFeatures, List<float[]> afterFlush, List<Float> distances)
	{
	}

	private record Report(String status, int comparisons, int green, int motion, List<Case> cases, List<String> failures)
	{
	}
}
