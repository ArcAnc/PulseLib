/**
 * @author ArcAnc
 * Created at: 30.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test.stage1;

import com.arcanc.pulselib.content.animatable.ControllerState;
import com.arcanc.pulselib.content.model.animation.PRawAnimation;
import com.arcanc.pulselib.content.player.animation.PPlayerAnimations;
import com.arcanc.pulselib.content.renderer.base.PWorldRenderState;
import com.arcanc.pulselib.content.renderer.gl.PGlRenderHost;
import com.arcanc.pulselib.util.PLibDatabase;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Same-frame native layer oracle; real opaque owners must clip particles/rain placed behind them. */
public final class Stage52AnimatedEffectsProbe
{
	private static final String[] KINDS = {"native-armor", "custom-armor", "anchor-opaque", "anchor-ordinary", "anchor-oit"};
	private static final String[] STATES = {"background", "owner", "effect-front", "both-front", "effect-behind", "both-behind"};
	private static final int[][] SAMPLES = new int[9][2];
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix4f ANCHOR = new Matrix4f();
	private static final List<Case> CASES = new ArrayList<>();
	private static final int[][][] SUBMISSIONS = new int[6][2][20];
	private static final int[][][] SELECTIONS = new int[6][2][20];
	private static List<float[]> base = List.of();
	private static List<List<float[]>> nativeLayers = List.of();
	private static List<float[]> nativeResolved = List.of();
	private static boolean started;
	private static boolean completed;
	private static boolean advance;
	private static boolean armed;
	private static boolean layersReady;
	private static boolean anchorSeen;
	private static int phase;
	private static int effect;
	private static int variant;
	private static int state;
	private static int configured = -1;
	private static int startTick;
	private static int frames;
	private static int comparisons;
	private static int motion;
	private static int formats;
	private static int green;
	private static final int[][][] COVERAGE = new int[6][2][3];
	private static long deadline;
	private static GraphicsPreset originalPreset = GraphicsPreset.CUSTOM;
	private static CameraType originalCamera = CameraType.FIRST_PERSON;
	private static boolean originalGui;
	private static boolean originalBob;
	private static @Nullable CompletableFuture<Void> reload;

	private Stage52AnimatedEffectsProbe()
	{
	}

	static boolean active()
	{
		return started && !completed;
	}

	static int kind()
	{
		return variant % 10 / 2;
	}

	static int format()
	{
		return variant / 10;
	}

	static void selectedArmor()
	{
		SELECTIONS[phase][effect][variant]++;
	}

	static void anchor(Matrix4f matrix)
	{
		ANCHOR.set(matrix);
		anchorSeen = true;
	}

	static void submitted()
	{
		SUBMISSIONS[phase][effect][variant]++;
	}

	static void register()
	{
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52AnimatedEffectsProbe::early);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52AnimatedEffectsProbe::layers);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Stage52AnimatedEffectsProbe::finish);
		NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) ->
		{
			check(completed && CASES.size() == 1440 && comparisons == 38880 && motion == 1080 && formats == 1080 && green == 1296,
					"Every animated particle/weather case, actual overlap and pixel/depth witness must finish");
			PLibDatabase.LOGGER.info("ANIMATED EFFECTS SHUTDOWN PASS");
		});
	}

	public static void tick()
	{
		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.getOverlay() != null || Stage52AnimatedEnvironmentProbe.RIGS.stream().anyMatch(rig -> rig.getModel() == null))
			return;
		if (!started)
		{
			check(Files.isRegularFile(mc.gameDirectory.toPath().resolve(".pulselib-animated-effects")), "Disposable effects client marker exists");
			for (var rig : Stage52AnimatedEnvironmentProbe.RIGS)
			{
				var clip = rig.getModel().animations().get("depth");
				check(clip != null && clip.length() == 20 && clip.boneAnimations().containsKey("body"), "Real decoded owner clip exists");
			}
			originalPreset = mc.options.graphicsPreset().get();
			originalCamera = mc.options.getCameraType();
			originalGui = mc.options.hideGui;
			originalBob = mc.options.bobView().get();
			mc.options.hideGui = true;
			mc.options.bobView().set(false);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			var owner = new RemotePlayer(mc.level, new GameProfile(new UUID(0, Stage52AnimatedEnvironmentProbe.PLAYER), "AnimatedEffects"));
			owner.setId(Stage52AnimatedEnvironmentProbe.PLAYER);
			mc.level.addEntity(owner);
			started = true;
			deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(20);
			mode();
		}
		check(System.nanoTime() < deadline, "Effects timeout: phase=" + phase + " effect=" + effect + " variant=" + variant + " state=" + state);
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
			if (++state == 6)
			{
				state = 0;
				if (++variant == 20)
				{
					variant = 0;
					if (++effect == 2)
					{
						effect = 0;
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
			}
			frames = 0;
			startTick = Math.max(startTick, mc.player.tickCount);
		}
		KeyMapping.releaseAll();
		mc.mouseHandler.releaseMouse();
		double x = state >= 4 ? 24.5 : 0.5;
		Stage52AnimatedEnvironmentProbe.place(mc.player, x, -3, 0);
		mc.level.setRainLevel(effect == 1 && state >= 2 ? 1 : 0);
		mc.level.setThunderLevel(0);
		var owner = (RemotePlayer)mc.level.getEntity(Stage52AnimatedEnvironmentProbe.PLAYER);
		check(owner != null, "Real native effects owner exists");
		if (configured != phase * 40 + effect * 20 + variant)
		{
			configured = phase * 40 + effect * 20 + variant;
			for (var id : Stage52AnimatedEnvironmentProbe.IDS)
				PPlayerAnimations.getHandle(owner, id).stopAll();
			owner.setItemSlot(EquipmentSlot.CHEST, kind() < 2 ? new ItemStack(Items.DIAMOND_CHESTPLATE) : ItemStack.EMPTY);
			check(PPlayerAnimations.getHandle(owner, Stage52AnimatedEnvironmentProbe.IDS.get(format())).play("depth", PRawAnimation.begin().thenLoop("depth").build()), "Real effects controller starts");
		}
		var controller = PPlayerAnimations.getHandle(owner, Stage52AnimatedEnvironmentProbe.IDS.get(format())).controller("depth");
		check(controller != null, "Real effects controller exists");
		controller.seek(Stage52AnimatedEnvironmentProbe.RIGS.get(format()).getModel(), variant % 2 * 10);
		controller.pause();
		check(controller.getState() == ControllerState.PAUSE && controller.getInterpolatedTime(0.5f) == variant % 2 * 10, "Hold actual decoded pose across controls");
		Stage52AnimatedEnvironmentProbe.place(owner, state % 2 == 0 ? x - 2 : x, 0, 180);
		if (effect == 0 && state >= 2)
			for (int sample = 0; sample < 9; sample++)
			{
				double z = state >= 4 ? 2 : -1.5;
				double scale = (z + 3) / 3;
				double eye = mc.player.getEyeY();
				var particle = mc.particleEngine.createParticle(ParticleTypes.SOUL, x + (sample % 3 - 1) * 0.16 * scale,
						eye + (81.05 + (sample / 3 - 1) * 0.12 - eye) * scale, z, 0, 0, 0);
				check(particle != null, "Native soul provider exists");
				particle.setLifetime(2);
				particle.setParticleSpeed(0, 0, 0);
			}
	}

	private static void mode()
	{
		var mc = Minecraft.getInstance();
		mc.options.graphicsPreset().set(GraphicsPreset.values()[phase % 3]);
		mc.levelRenderer.allChanged();
		startTick = mc.player.tickCount + 40;
		frames = 0;
		armed = layersReady = false;
	}

	private static void early(RenderLevelStageEvent.AfterOpaqueFeatures event)
	{
		armed = layersReady = false;
		nativeResolved = List.of();
		var mc = Minecraft.getInstance();
		if (!active() || advance || reload != null || mc.getOverlay() != null || mc.player.tickCount < startTick + 3 || ++frames < 4)
			return;
		check(mc.player.tickCount < startTick + 1200, "Native effect coverage not ready: phase=" + phase + " effect=" + effect + " variant=" + variant + " state=" + state);
		double x = mc.player.getX();
		if (!mc.levelRenderer.isSectionCompiledAndVisible(BlockPos.containing(x, 81, 0)) || !mc.levelRenderer.isSectionCompiledAndVisible(BlockPos.containing(x, 81, 6)))
			return;
		var owner = event.getLevelRenderState().entityRenderStates.stream().filter(value -> value instanceof AvatarRenderState avatar && avatar.id == Stage52AnimatedEnvironmentProbe.PLAYER).findFirst().orElse(null);
		if (owner == null)
			return;
		check(event.getLevelRenderState().entityRenderStates.stream().allMatch(value -> value instanceof AvatarRenderState), "Disposable effects scene contains no foreign entity render states");
		check(((PWorldRenderState)owner).pulselib$requiresSeparateTargets(), "Actual extracted owner retains native effect targets");
		check(Minecraft.useShaderTransparency() == (phase % 3 == 2), "Requested native graphics mode remains selected");
		var frame = new PGlRenderHost().captureFrame();
		PROJECTION.set(frame.projection());
		VIEW.set(frame.modelView());
		var vp = new Matrix4f(PROJECTION).mul(VIEW);
		var camera = event.getLevelRenderState().cameraRenderState.pos;
		var target = mc.getMainRenderTarget();
		for (int sample = 0; sample < 9; sample++)
		{
			var screen = vp.transform(new Vector4f((float)(x + (sample % 3 - 1) * 0.16 - camera.x),
					(float)(81.05 + (sample / 3 - 1) * 0.12 - camera.y), (float)-camera.z, 1));
			SAMPLES[sample][0] = (int)((screen.x / screen.w + 1) * 0.5f * target.width);
			SAMPLES[sample][1] = (int)((screen.y / screen.w + 1) * 0.5f * target.height);
		}
		armed = true;
		layersReady = false;
		nativeResolved = List.of();
	}

	private static void layers(RenderLevelStageEvent.AfterWeather event)
	{
		if (!armed || advance)
			return;
		var mc = Minecraft.getInstance();
		var renderer = mc.levelRenderer;
		RenderTarget[] targets = {renderer.getTranslucentTarget(), renderer.getItemEntityTarget(), renderer.getParticlesTarget(), renderer.getWeatherTarget(), renderer.getCloudsTarget()};
		List<List<float[]>> captured = new ArrayList<>();
		for (var target : targets)
		{
			check(target != null, "Every native effect target survives until real composition: phase=" + phase + " effect=" + effect + " variant=" + variant + " state=" + state + " target=" + captured.size());
			captured.add(pixels(target));
		}
		base = pixels(mc.getMainRenderTarget());
		nativeLayers = captured;
		int covered = 0;
		for (int sample = 0; sample < 9; sample++)
		{
			float[] value = captured.get(effect + 2).get(sample);
			if (state >= 2 && value[3] > 0.005f)
			{
				float ownerDistance = CASES.get(CASES.size() - (state - 1)).pixels.get(sample).ownerDistance;
				check(value[4] < 1, "Actual native effect pixels must write fragment depth");
				check(state < 4 ? distance(value[4]) < ownerDistance - 0.5f : distance(value[4]) > ownerDistance + 0.5f,
						"Real effect depth must lie on the requested side: effect=" + effect + " state=" + state + " sample=" + sample + " source=" + distance(value[4]) + " owner=" + ownerDistance);
				covered++;
			}

		}
		if (state >= 2 && state <= 4 && covered == 0)
			return;
		if (state >= 2 && state <= 4)
			COVERAGE[phase][effect][state - 2] += covered;
		layersReady = true;
	}

	public static void beforeComposite(RenderTarget target)
	{
		if (armed && layersReady && !advance)
			nativeResolved = pixels(target);
	}

	private static void finish(RenderLevelStageEvent.AfterLevel event)
	{
		if (!armed || !layersReady || advance)
			return;
		var mc = Minecraft.getInstance();
		var result = pixels(mc.getMainRenderTarget());
		check(nativeResolved.size() == 9, "Read the real native postchain before production OIT insertion");
		List<Pixel> pixels = new ArrayList<>();
		float[] meshDepths = meshDepths();
		for (int sample = 0; sample < 9; sample++)
		{
			List<Layer> layers = new ArrayList<>();
			layers.add(new Layer(base.get(sample)[4], new float[]{base.get(sample)[0], base.get(sample)[1], base.get(sample)[2], 1}));
			List<float[]> inputs = new ArrayList<>();
			for (var source : nativeLayers)
			{
				float[] value = source.get(sample);
				inputs.add(value);
				if (value[3] > 0)
					layers.add(new Layer(value[4], new float[]{value[0] / value[3], value[1] / value[3], value[2] / value[3], value[3]}));
			}
			float[] nativeExpected = resolve(layers);
			if (kind() == 4 && state % 2 == 1)
			{
				check(anchorSeen, "Real attachment callback supplies the source model transform");
				for (float depth : meshDepths)
				{
					check(depth < base.get(sample)[4], "Both authored cube faces lie in front of native skin depth");
					layers.add(new Layer(depth, new float[]{0, 1, 0, 128f / 255}));
				}
			}
			float[] expected = resolve(layers);
			pixels.add(new Pixel(SAMPLES[sample][0], SAMPLES[sample][1], base.get(sample), inputs, nativeResolved.get(sample), result.get(sample), nativeExpected, expected,
					distance(Math.min(result.get(sample)[4], inputs.get(1)[4]))));
		}
		CASES.add(new Case(phase, effect == 0 ? "soul" : "rain", variant, KINDS[kind()], format() == 0 ? "gecko" : "gltf", variant % 2 * 10, state, STATES[state],
				mc.options.graphicsPreset().get().toString(), Minecraft.useShaderTransparency(), PROJECTION.get(new float[16]), VIEW.get(new float[16]), ANCHOR.get(new float[16]), meshDepths, pixels));
		if (state == 1 || state == 5)
			writeReport();
		for (int sample = 0; sample < 9; sample++)
		{
			var pixel = pixels.get(sample);
			verify(Stage52AnimatedEnvironmentProbe.rgb(pixel.nativeExpected, pixel.nativeResolved) < 0.025f, "Native effect resolve RGB", sample, pixel.nativeExpected, pixel.nativeResolved);
			verify(Stage52AnimatedEnvironmentProbe.rgb(pixel.expected, pixel.result) < 0.035f, "Ordered animated effect RGB", sample, pixel.expected, pixel.result);
			verify(Math.abs(pixel.base[4] - pixel.result[4]) < 0.000001f, "Effect composition preserves destination depth", sample, pixel.base, pixel.result);
			if (state == 1)
			{
				if (variant % 2 == 1)
				{
					var previous = CASES.get(CASES.size() - 7).pixels.get(sample);
					check(Math.abs(pixel.ownerDistance - previous.ownerDistance) > 0.15f, "Decoded animation changes actual owner GPU depth");
					motion++;
				}
				if (format() == 1)
				{
					var gecko = CASES.get(CASES.size() - 61).pixels.get(sample);
					check(Math.abs(pixel.ownerDistance - gecko.ownerDistance) < 0.002f, "Actual Gecko/glTF owner surfaces match");
					formats++;
				}
				if (kind() >= 2)
				{
					check(pixel.result[1] > pixel.result[0] + 0.1f && pixel.result[1] > pixel.result[2] + 0.1f, "Actual socket emits green pixels");
					green++;
				}
			}
		}
		if (state == 5)
		{
			writeReport();
			PLibDatabase.LOGGER.info("ANIMATED EFFECTS GROUP PASS: phase={}, effect={}, variant={}, cases={}, comparisons={}, motion={}, formats={}, green={}", phase, effect, variant, CASES.size(), comparisons, motion, formats, green);
			if (phase == 2 && variant == 18)
				Screenshot.grab(mc.gameDirectory, "animated-effect-" + effect + ".png", mc.getMainRenderTarget(), 1,
						message -> PLibDatabase.LOGGER.info("{}", message.getString()));
		}
		armed = layersReady = anchorSeen = false;
		advance = true;
	}

	private static float[] meshDepths()
	{
		var mvp = new Matrix4f(PROJECTION).mul(VIEW).mul(ANCHOR);
		float[] values = new float[2];
		for (int face = 0; face < 2; face++)
		{
			var clip = mvp.transform(new Vector4f(0, 1, face == 0 ? -0.5f : 0.5f, 1));
			values[face] = (clip.z / clip.w + 1) * 0.5f;
		}
		return values;
	}

	private static float[] resolve(List<Layer> layers)
	{
		layers.sort(Comparator.comparingDouble(Layer::depth).reversed());
		float[] color = new float[3];
		for (var layer : layers)
			for (int channel = 0; channel < 3; channel++)
				color[channel] = layer.color[channel] * layer.color[3] + color[channel] * (1 - layer.color[3]);
		return color;
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
			values.add(Stage52AnimatedEnvironmentProbe.pixel(target, sample[0], sample[1]));
		return values;
	}

	private static void verify(boolean condition, String message, int sample, float[] expected, float[] actual)
	{
		comparisons++;
		if (!condition)
			writeReport();
		check(condition, message + " phase=" + phase + " effect=" + effect + " variant=" + variant + " state=" + state + " sample=" + sample + " expected=" + java.util.Arrays.toString(expected) + " actual=" + java.util.Arrays.toString(actual));
	}

	private static void complete()
	{
		for (int mode = 0; mode < 6; mode++)
			for (int source = 0; source < 2; source++)
				for (int owner = 0; owner < 20; owner++)
				{
					if (owner % 10 / 2 >= 2)
						check(SUBMISSIONS[mode][source][owner] > 0, "Every attachment group actually submits");
					if (owner % 10 / 2 == 1)
						check(SELECTIONS[mode][source][owner] > 0, "Every custom armor group selects its real model");
				}
		completed = true;
		writeReport();
		var mc = Minecraft.getInstance();
		mc.options.graphicsPreset().set(originalPreset);
		mc.options.setCameraType(originalCamera);
		mc.options.hideGui = originalGui;
		mc.options.bobView().set(originalBob);
		mc.level.setRainLevel(0);
		Stage52AnimatedEnvironmentProbe.clearCustomModels();
		if (Boolean.getBoolean("pulselib.stage2Probe"))
			Stage2PluginProbe.verify(true);
		PLibDatabase.LOGGER.info("ANIMATED EFFECTS PASS: cases={}, comparisons={}, motion={}, formats={}, green={}, coverage={}", CASES.size(), comparisons, motion, formats, green, java.util.Arrays.deepToString(COVERAGE));
		mc.stop();
	}

	private static void writeReport()
	{
		try
		{
			Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("animated-effects-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
					new Report(completed ? "PASS" : "INCOMPLETE", comparisons, motion, formats, green, SUBMISSIONS, SELECTIONS, COVERAGE, CASES)));
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

	private record Layer(float depth, float[] color)
	{
	}

	private record Pixel(int x, int y, float[] base, List<float[]> nativeLayers, float[] nativeResolved, float[] result, float[] nativeExpected, float[] expected, float ownerDistance)
	{
	}

	private record Case(int phase, String effect, int variant, String owner, String format, int ticks, int control, String state, String preset, boolean improved, float[] projection, float[] view, float[] anchor, float[] meshDepths, List<Pixel> pixels)
	{
	}

	private record Report(String status, int comparisons, int motion, int formats, int green, int[][][] submissions, int[][][] selections, int[][][] coverage, List<Case> cases)
	{
	}
}
