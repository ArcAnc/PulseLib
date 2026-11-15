/**
 * @author ArcAnc
 * Created at: 15.11.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.porting.scene;

import com.arcanc.pulselib.content.model.baked.PBakedMesh;
import com.arcanc.pulselib.content.model.deformer.PChannelReference;
import com.arcanc.pulselib.content.model.deformer.PDeformerInstance;
import com.arcanc.pulselib.content.model.deformer.PDeformerStack;
import com.arcanc.pulselib.content.model.deformer.PMeshDeformation;
import com.arcanc.pulselib.content.model.deformer.PStretchDefinition;
import com.arcanc.pulselib.content.model.deformer.PStretchDeformer;
import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import com.arcanc.pulselib.content.renderer.rhi.PRhiRenderHost;
import com.arcanc.pulselib.porting.scene.WorldReadback.Samples;
import com.arcanc.pulselib.util.PRenderTypes;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Real integrated world/vanilla framegraph acceptance, only inside a fresh Gradle scene directory. */
public final class WorldChecks
{
	private static final Identifier WHITE = Identifier.fromNamespaceAndPath("pulselib_client_scene", "world_white");
	private static final PBakedMesh MESH = SceneChecks.baked(SceneChecks.geometry(0), WHITE, false, true);
	private static final PDeformerStack STRETCH = PDeformerStack.compile(List.of(new PDeformerInstance<>(PStretchDeformer.INSTANCE,
			new PStretchDefinition(new Vector3f(), new Vector3f(1, 0, 0), PChannelReference.constant(0.5f)))));
	private static final String[] PHASES = {"fabulous-dry", "fabulous-rain", "opaque-occlusion", "ordinary-rain", "resize-fabulous", "reload-fabulous", "clouds-fabulous", "reopen-fabulous"};
	private static final int[] COLORS = {0x80FFFF00, 0x800000FF, 0x8000FF00, 0x80FF0000};
	private static final int[] vanillaPixels = new int[5];
	private static final List<Map<String, Object>> results = new ArrayList<>();
	private static final List<Layer> layers = new ArrayList<>();
	private static int phase = -1;
	private static int warm;
	private static int checked;
	private static int submissions;
	private static int probeX;
	private static float factor;
	private static boolean started;
	private static boolean suspended;
	private static boolean done;
	private static boolean reopening;
	private static boolean disconnected;
	private static boolean abortIssued;
	private static boolean abortRecovered;
	private static int hiddenLayers;
	private static volatile boolean setup;
	private static double anchorX, anchorY, anchorZ;
	private static @Nullable CompletableFuture<Void> reload;
	private static @Nullable Throwable failure;
	private static @Nullable GpuTextureView sampledTarget;
	private static @Nullable Samples before;
	private static @Nullable Samples opaque;
	private static final List<Front> fronts = new ArrayList<>();

	private WorldChecks()
	{
	}

	public static void tick()
	{
		if (done)
			return;
		var client = Minecraft.getInstance();
		try
		{
			if (failure != null)
				throw new AssertionError("World framegraph acceptance", failure);
			if (!client.isGameLoadFinished() || client.gui.overlay() != null)
				return;
			if (!started)
			{
				SceneChecks.check(Files.isRegularFile(Path.of("mods/pulselib-client-scene.jar")), "Only an isolated Gradle directory may create a world");
				SceneChecks.check(RenderSystem.getDevice().getDeviceInfo().backendName().equalsIgnoreCase(System.getProperty("pulselib.scene.backend")), "No backend fallback allowed");
				WorldResourceChecks.run();
				client.options.pauseOnLostFocus = false;
				client.options.renderDistance().set(3);
				client.options.simulationDistance().set(5);
				client.options.cloudStatus().set(CloudStatus.FANCY);
				registerTexture();
				started = true;
				client.schedule(() -> client.createWorldOpenFlows().createFreshLevel("pulselib-isolated-world",
						new LevelSettings("PulseLib isolated OIT", GameType.SPECTATOR, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
						new WorldOptions(12345, false, false), provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen()));
				return;
			}
			if (phase == 7 && reopening && !disconnected)
				return;
			if (phase == 7 && reopening && client.level == null)
			{
				reopening = false;
				var field = PRenderQueue.class.getDeclaredField("OIT_EXECUTOR");
				field.setAccessible(true);
				var executor = (com.arcanc.pulselib.content.renderer.rhi.PRhiOitRenderer)field.get(null);
				var resources = executor.getClass().getDeclaredField("resources");
				resources.setAccessible(true);
				SceneChecks.check(resources.get(executor) == null && !executor.hasPending(), "Actual disconnect closes OIT registry and drops pending plans");
				client.schedule(() -> client.createWorldOpenFlows().openWorld("pulselib-isolated-world", () -> fail(new AssertionError("Reopen cancelled"))));
				return;
			}
			if (client.level == null || client.player == null || client.gui.screen() != null)
				return;
			if (phase < 0)
			{
				anchorX = client.player.getX();
				anchorY = client.player.getY();
				anchorZ = client.player.getZ();
				var server = Objects.requireNonNull(client.getSingleplayerServer());
				server.execute(() ->
				{
					var level = server.overworld();
					for (int x = -3; x <= 3; x++)
						for (int y = 0; y < 7; y++)
							level.setBlockAndUpdate(BlockPos.containing(anchorX + x, anchorY + y, anchorZ + 7), Blocks.GLASS.defaultBlockState());
					var item = new ItemEntity(level, anchorX, anchorY + 2.8, anchorZ + 4, new ItemStack(Blocks.SLIME_BLOCK), 0, 0, 0);
					item.setNoGravity(true);
					item.setNeverPickUp();
					level.addFreshEntity(item);
					setup = true;
				});
				enter(0);
			}
			if (!setup)
				return;
			if (reload != null)
			{
				if (!reload.isDone())
					return;
				reload.join();
				reload = null;
				registerTexture();
				suspended = false;
				warm = 0;
			}
			if (phase == 7)
				suspended = false;
			client.player.setYRot(0);
			client.player.setXRot(phase == 6 ? -65 : -20);
			client.level.setRainLevel(phase == 0 ? 0 : 1);
			if (phase > 0)
				for (int i = 0; i < 3; i++)
					client.level.addParticle(ParticleTypes.CLOUD, anchorX + (i - 1) * 0.4, anchorY + 2.8, anchorZ + 2.5, 0, 0.01, 0);
			if (++warm < 45 || checked < 6)
				return;
			System.out.println("PASS world phase " + PHASES[phase] + " pixels=" + checked + " native vanilla coverage=" + java.util.Arrays.toString(vanillaPixels));
			if (phase == 2)
				SceneChecks.check(hiddenLayers >= 3, "Opaque stone must hide the farther three OIT layers");
			if (phase == PHASES.length - 1)
			{
				SceneChecks.check(abortRecovered, "A real framegraph composite failure must recover");
				for (int i = 0; i < vanillaPixels.length; i++)
					SceneChecks.check(vanillaPixels[i] > 0, "Actual vanilla target never rendered slot=" + i);
				Files.writeString(Path.of("pulselib-world-evidence.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("phases", results, "vanillaCoverage", vanillaPixels,
						"backend", RenderSystem.getDevice().getDeviceInfo().backendName(), "submissions", submissions, "abortRecovered", abortRecovered)));
				Files.writeString(Path.of("pulselib-scene-result.txt"), "PASS\n");
				done = true;
				client.stop();
			}
			else
				enter(phase + 1);
		}
		catch (Throwable error)
		{
			error.printStackTrace();
			try
			{
				Files.writeString(Path.of("pulselib-scene-result.txt"), "FAIL: " + error + "\n");
			}
			catch (Exception recording)
			{
				error.addSuppressed(recording);
			}
			done = true;
			client.stop();
		}
	}

	private static void registerTexture()
	{
		var image = new NativeImage(1, 1, false);
		image.setPixel(0, 0, -1);
		Minecraft.getInstance().getTextureManager().register(WHITE, new DynamicTexture(() -> "World OIT fixture", image));
	}

	private static void enter(int next)
	{
		phase = next;
		warm = checked = hiddenLayers = 0;
		var client = Minecraft.getInstance();
		client.options.improvedTransparency().set(phase != 3);
		var server = Objects.requireNonNull(client.getSingleplayerServer());
		server.execute(() ->
		{
			var weather = server.overworld().getWeatherData();
			weather.setClearWeatherTime(0);
			weather.setRainTime(100000);
			weather.setRaining(phase != 0);
			if (phase == 2 || phase == 3)
				for (int x = -3; x <= 3; x++)
					for (int y = 0; y < 7; y++)
						server.overworld().setBlockAndUpdate(BlockPos.containing(anchorX + x, anchorY + y, anchorZ + 3),
								(phase == 2 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
		});
		if (phase == 4)
			client.getWindow().setWindowed(640, 360);
		if (phase == 5)
		{
			suspended = true;
			reload = client.reloadResourcePacks();
		}
		if (phase == 7)
		{
			suspended = true;
			reopening = true;
			client.schedule(() ->
			{
				client.disconnectWithSavingScreen();
				disconnected = true;
			});
		}
	}

	public static void submit()
	{
		if (!Boolean.getBoolean("pulselib.scene.world") || done || suspended || phase < 0 || Minecraft.getInstance().level == null)
			return;
		try
		{
			var frame = new PRhiRenderHost().captureFrame();
			factor = submissions % 2 == 0 ? 0.3f : 0.9f;
			float submitted = factor;
			var point = frame.projection().transform(new Vector4f(0.25f, 0, -3.5f, 1));
			var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
			probeX = Math.round((point.x / point.w + 1) * 0.5f * target.width);
			if (warm >= 30 && checked < 6)
				opaque = WorldReadback.read(Objects.requireNonNull(target.getDepthTexture()), probeX, null);
			layers.clear();
			for (int i = 0; i < COLORS.length; i++)
			{
				float distance = 5.5f - i;
				var projected = frame.projection().transform(new Vector4f(0, 0, -distance, 1));
				layers.add(new Layer(projected.z / projected.w, COLORS[i]));
				PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(WHITE), MESH,
						new PMeshDeformation(STRETCH, reference -> submitted, new Object(), 0),
						new PRenderQueue.InstanceData(new Matrix4f(frame.modelView()).invert().translate(0, 0, -distance - 0.5f), COLORS[i], 0xF000F0, OverlayTexture.NO_OVERLAY), PTransparencyMode.OIT);
			}
			submissions++;
		}
		catch (Throwable error)
		{
			fail(error);
		}
	}

	public static void beforeComposite(GpuTextureView destination, List<RenderTarget> vanilla)
	{
		if (!Boolean.getBoolean("pulselib.scene.world") || done || suspended || warm < 30 || checked >= 6)
			return;
		try
		{
			SceneChecks.check(vanilla.size() == (phase == 3 ? 0 : 5), "Actual option must select native framegraph inputs");
			sampledTarget = destination;
			before = WorldReadback.read(destination.texture(), probeX, null);
			fronts.clear();
			for (int i = 0; i < vanilla.size(); i++)
			{
				var color = WorldReadback.read(Objects.requireNonNull(vanilla.get(i).getColorTexture()), probeX, null);
				var depth = WorldReadback.read(Objects.requireNonNull(vanilla.get(i).getDepthTexture()), probeX, null);
				vanillaPixels[i] = Math.max(vanillaPixels[i], color.nonempty());
				fronts.add(new Front(color, depth));
			}
		}
		catch (Throwable error)
		{
			fail(error);
		}
	}

	public static void afterComposite()
	{
		if (before == null || sampledTarget == null)
			return;
		try
		{
			var actual = WorldReadback.read(sampledTarget.texture(), probeX, checked == 0 ? "world-" + PHASES[phase] + ".png" : null);
			hiddenLayers = 0;
			var expected = reference(false);
			var probe = reference(true);
			compare(actual.center(), expected, "center");
			compare(actual.probe(), probe, "sampled silhouette scale=" + factor);
			results.add(Map.of("phase", PHASES[phase], "scale", factor, "center", WorldReadback.packed(actual.center()), "expected", WorldReadback.packed(expected),
					"probe", WorldReadback.packed(actual.probe()), "probeExpected", WorldReadback.packed(probe), "width", sampledTarget.getWidth(0), "height", sampledTarget.getHeight(0), "hiddenLayers", hiddenLayers));
			checked++;
		}
		catch (Throwable error)
		{
			fail(error);
		}
		finally
		{
			before = null;
			sampledTarget = null;
		}
	}

	private static float[] reference(boolean probe)
	{
		var color = (probe ? Objects.requireNonNull(before).probe() : Objects.requireNonNull(before).center()).clone();
		float opaqueDepth = probe ? Objects.requireNonNull(opaque).probe()[0] : Objects.requireNonNull(opaque).center()[0];
		if (probe && factor < 0.5f)
			return color;
		for (var layer : layers)
		{
			if (layer.depth() < opaqueDepth)
			{
				if (!probe)
					hiddenLayers++;
				continue;
			}
			List<float[]> near = new ArrayList<>();
			for (var input : fronts)
			{
				float[] nativeColor = probe ? input.color().probe() : input.color().center();
				float depth = probe ? input.depth().probe()[0] : input.depth().center()[0];
				if (nativeColor[3] > 0 && depth > layer.depth())
					near.add(new float[]{nativeColor[0], nativeColor[1], nativeColor[2], nativeColor[3], depth});
			}
			near.sort(Comparator.comparingDouble(item -> item[4]));
			float[] front = new float[3];
			float transmission = 1;
			for (var item : near)
			{
				for (int c = 0; c < 3; c++)
					front[c] = front[c] * (1 - item[3]) + item[c];
				transmission *= 1 - item[3];
			}
			float alpha = (layer.color() >>> 24) / 255f;
			for (int c = 0; c < 3; c++)
				color[c] = (front[c] + transmission * (layer.color() >> (16 - c * 8) & 255) / 255f) * alpha + color[c] * (1 - alpha);
			if (fronts.isEmpty())
				color[3] = alpha + color[3] * (1 - alpha);
		}
		return color;
	}

	private static void compare(float[] actual, float[] expected, String sample)
	{
		for (int c = 0; c < 4; c++)
			SceneChecks.check(Math.abs(actual[c] - expected[c]) <= 3f / 255, "World " + PHASES[phase] + " " + sample + " actual=" + Integer.toHexString(WorldReadback.packed(actual)) + " expected=" + Integer.toHexString(WorldReadback.packed(expected)));
	}

	public static boolean abortRequested()
	{
		if (!Boolean.getBoolean("pulselib.scene.world") || phase != 0 || submissions < 10 || abortIssued)
			return false;
		abortIssued = true;
		return true;
	}

	public static void recoveredAbort(Matrix4f matrix)
	{
		try
		{
			SceneChecks.check(RenderSystem.getModelViewMatrixCopy().equals(matrix), "Aborted world restores the caller model-view matrix");
			SceneChecks.check(com.arcanc.pulselib.content.model.deformer.gpu.PGpuDeformerBuffers.values().isEmpty(), "Framegraph finally clears aborted sampled values");
			var field = PRenderQueue.class.getDeclaredField("OIT_EXECUTOR");
			field.setAccessible(true);
			SceneChecks.check(!((com.arcanc.pulselib.content.renderer.rhi.PRhiOitRenderer)field.get(null)).hasPending(), "Framegraph finally discards aborted plans");
			abortRecovered = true;
			System.out.println("PASS actual framegraph OIT exception: original finally clears pending plans/values; next world frames continue");
		}
		catch (Throwable error)
		{
			fail(error);
		}
	}

	public static final class ExpectedWorldAbort extends RuntimeException
	{
	}

	private static void fail(Throwable error)
	{
		if (failure == null)
			failure = error;
	}

	private record Layer(float depth, int color)
	{
	}

	private record Front(Samples color, Samples depth)
	{
	}
}
