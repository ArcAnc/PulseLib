/**
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * <p>
 * This code is licensed under "Arc's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package com.arcanc.pulselib.test;

import com.arcanc.pulselib.content.renderer.PRenderQueue;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CameraType;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.arcanc.pulselib.test.SmokeHarness.check;
import static com.arcanc.pulselib.test.SmokeHarness.field;

/** Measures the entire GameRenderer GPU interval in one fixed disposable world. */
public final class WorldOitPerformanceHarness
{
	private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("pulselibtest", "world_perf_white");
	private static final int WARMUP = 64;
	private static final int SAMPLES = 80;
	private static final List<GpuTiming.Result> results = new ArrayList<>();
	private static final List<String> rows = new ArrayList<>();
	private static @Nullable CompletableFuture<Void> setup;
	private static @Nullable PGeometryData geometry;
	private static int configuration;
	private static int frames;
	private static int originalWidth;
	private static int originalHeight;
	private static boolean ready;
	private static boolean capturing;
	private static boolean completed;
	private static @Nullable List<Integer> warmResourceIds;

	private WorldOitPerformanceHarness()
	{
	}

	public static void beginFrame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (!ready || completed || mc.player == null || mc.getOverlay() != null || mc.screen != null)
		{
			return;
		}
		KeyMapping.releaseAll();
		mc.player.setPos(10.5, 140.0, 10.5);
		mc.player.xo = mc.player.xOld = 10.5;
		mc.player.yo = mc.player.yOld = 140.0;
		mc.player.zo = mc.player.zOld = 10.5;
		mc.player.setYRot(0f);
		mc.player.setXRot(0f);
		mc.player.yRotO = mc.player.xRotO = 0f;
		mc.player.setDeltaMovement(0.0, 0.0, 0.0);
		if (frames < WARMUP + SAMPLES)
		{
			if (frames == WARMUP && quads() > 0)
			{
				warmResourceIds = OitPerformanceHarness.resourceIds();
			}
			GpuTiming.start();
			capturing = true;
		}
	}

	public static void endFrame()
	{
		if (capturing)
		{
			GpuTiming.finish(frames >= WARMUP);
			frames++;
			capturing = false;
		}
	}

	public static void queue()
	{
		if (!ready || completed)
		{
			return;
		}
		var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		for (int index = 0; index < quads(); index++)
		{
			PRenderQueue.submitEntityMesh(PRenderTypes.RenderTypeProvider.trianglesTranslucentEmissive(WHITE), geometry,
					new PInstanceHeader(new Matrix4f().translation((float)(11.0 - camera.x), (float)(142.0 - camera.y),
							(float)(15.0 + (index % 4) * 0.25 - camera.z)).scale(4f, 2f, 1f),
						0x4080C0FF, 0xF000F0, OverlayTexture.NO_OVERLAY));
		}
	}

	public static void frame()
	{
		Minecraft mc = Minecraft.getInstance();
		if (completed || mc.level == null || mc.player == null || mc.getOverlay() != null)
		{
			return;
		}
		try
		{
			if (setup == null)
			{
				DisposableTestWorlds.requireCurrentWorld();
				IntBuffer width = BufferUtils.createIntBuffer(1);
				IntBuffer height = BufferUtils.createIntBuffer(1);
				GLFW.glfwGetWindowSize(mc.getWindow().getWindow(), width, height);
				originalWidth = width.get(0);
				originalHeight = height.get(0);
				GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
				mc.options.renderDistance().set(6);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.hideGui = true;
				NativeImage white = new NativeImage(1, 1, false);
				white.setPixelRGBA(0, 0, -1);
				mc.getTextureManager().register(WHITE, new DynamicTexture(white));
				geometry = H14Harness.quad();
				var server = mc.getSingleplayerServer();
				var uuid = mc.player.getUUID();
				setup = CompletableFuture.runAsync(() ->
				{
					var player = server.getPlayerList().getPlayer(uuid);
					check(player != null, "World profile needs a server player");
					var level = player.serverLevel();
					for (int x = 5; x <= 16; x++)
					{
						for (int y = 138; y <= 146; y++)
						{
							for (int z = 5; z <= 21; z++)
							{
								level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
							}
						}
					}
					for (int x = 8; x <= 10; x++)
					{
						for (int y = 140; y <= 144; y++)
						{
							level.setBlockAndUpdate(new BlockPos(x, y, 17), Blocks.STONE.defaultBlockState());
						}
					}
					level.setDayTime(6000L);
					level.setWeatherParameters(6000, 0, false, false);
					player.setGameMode(GameType.SPECTATOR);
					player.teleportTo(level, 10.5, 140.0, 10.5, 0f, 0f);
				}, server);
				rows.add("mode,width,height,quads,samples,frame_mean_ms,frame_p95_ms,depth_ms,accumulation_ms,metadata_ms,compositor_ms,oit_texture_mib,metadata_mib,oit_fullscreen_draws,steady_texture_allocations,buffer_data_calls");
				return;
			}
			if (!setup.isDone())
			{
				return;
			}
			setup.join();
			if (!ready)
			{
				check(mc.getMainRenderTarget().width == 1920 && mc.getMainRenderTarget().height == 1080,
						"World profile requires an actual 1920x1080 framebuffer");
				configure();
				ready = true;
				return;
			}
			boolean drained = GpuTiming.drain(results);
			if (frames >= WARMUP + SAMPLES && drained)
			{
				writeRow();
				configuration++;
				if (configuration == 6)
				{
					Path output = Path.of(System.getProperty("pulselib.test.oitOutput", "h14-results"));
					Files.createDirectories(output);
					Files.write(output.resolve("world-gpu-performance.csv"), rows);
					PLibDatabase.LOGGER.info("H14 WORLD GPU PERFORMANCE PASS: six 1080p scenarios, 64 warmup + 80 samples");
					completed = true;
					GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), originalWidth, originalHeight);
					mc.stop();
					return;
				}
				configure();
			}
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot write world GPU profile", exception);
		}
	}

	private static void configure()
	{
		Minecraft mc = Minecraft.getInstance();
		mc.options.graphicsMode().set(configuration < 2 ? GraphicsStatus.FANCY : GraphicsStatus.FABULOUS);
		mc.levelRenderer.graphicsChanged();
		frames = 0;
		results.clear();
		warmResourceIds = null;
	}

	private static int quads()
	{
		return new int[]{0, 1, 0, 1, 16, 128}[configuration];
	}

	private static void writeRow()
	{
		check(results.size() == SAMPLES, "Every world GPU sample must retire");
		check(warmResourceIds == null || warmResourceIds.equals(OitPerformanceHarness.resourceIds()),
				"World render buffers and textures must be reused after warmup");
		int draws = quads() == 0 ? 0 : configuration < 2 ? 4 : 8;
		check(results.stream().allMatch(result -> result.fullscreen() == draws && result.allocations() == 0),
				"World OIT must have the expected passes and stable resources");
		Object executor = field(PRenderQueue.class, "EXECUTOR", null);
		Object oit = field(executor.getClass(), "weightedBlendedOit", executor);
		long bytes = 0L;
		for (String name : List.of("accumulationTextures", "revealageTextures", "layerDepthTextures"))
		{
			for (int texture : (int[])field(oit.getClass(), name, oit))
			{
				bytes += OitPerformanceHarness.textureBytes(texture);
			}
		}
		long metadata = OitPerformanceHarness.textureBytes((Integer)field(oit.getClass(), "fabulousMetadataTexture", oit));
		double[] sorted = results.stream().mapToDouble(result -> result.milliseconds().get("frame")).sorted().toArray();
		String row = String.format(Locale.ROOT, "%s,1920,1080,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.3f,%.3f,%d,0,%.3f",
				configuration < 2 ? "Fancy" : "Fabulous", quads(), SAMPLES, mean("frame"), sorted[75],
				mean("depth"), mean("accumulation"), mean("metadata"), mean("compositor"),
				(bytes + metadata) / 1048576.0, metadata / 1048576.0, draws,
				results.stream().mapToInt(GpuTiming.Result::bufferReallocations).average().orElseThrow());
		rows.add(row);
		PLibDatabase.LOGGER.info("H14 WORLD GPU ROW {}", row);
	}

	private static double mean(String name)
	{
		return results.stream().mapToDouble(result -> result.milliseconds().getOrDefault(name, 0.0)).average().orElseThrow();
	}
}
