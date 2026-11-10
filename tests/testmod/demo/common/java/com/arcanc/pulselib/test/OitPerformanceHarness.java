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
import com.arcanc.pulselib.content.renderer.legacy.GlRenderState;
import com.arcanc.pulselib.content.renderer.plan.PTransparencyMode;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.ProjectionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.NVXGPUMemoryInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.arcanc.pulselib.test.RegressionChecks.check;
import static com.arcanc.pulselib.test.RegressionChecks.field;

/**
 * Warm, persistent offscreen fixtures at real render resolutions. No glFinish,
 * pixel readback or CPU timer occurs in measured frames. This measures the
 * fixture's GPU frame, not terrain/GUI/presentation in a normal Minecraft frame.
 */
public final class OitPerformanceHarness
{
	private static final Identifier WHITE = H14OffscreenHarness.WHITE;
	private static final String[] TARGETS = {"translucent", "itemEntity", "particles", "weather", "clouds"};
	private static final int WARMUP = 32;
	private static final int SAMPLES = 80;
	private static final List<GpuTiming.Result> results = new ArrayList<>();
	private static final List<String> rows = new ArrayList<>();
	private static @Nullable TextureTarget main;
	private static @Nullable ProjectionMatrixBuffer identity;
	private static @Nullable PerfTargets chain;
	private static @Nullable PGeometryData geometry;
	private static int configuration;
	private static int frame;
	private static boolean completed;
	private static boolean emptySubmitted;
	private static long sampleDeadline;
	private static @Nullable List<Integer> warmResourceIds;
	private static long initialAvailableVram;

	private OitPerformanceHarness()
	{
	}

	public static void frame()
	{
		if (Boolean.getBoolean("pulselib.test.world"))
		{
			WorldOitPerformanceHarness.frame();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (completed || mc.getOverlay() != null || PModelCache.getModels() == null)
			return;
		check(mc.level == null, "Offscreen profiling cannot run while a world is loaded");
		try
		{
			if (main == null)
				configure();
			boolean drained = GpuTiming.drain(results);
			if (frame >= WARMUP + SAMPLES)
			{
				check(System.nanoTime() < sampleDeadline, "GPU timer results did not become available");
				if (!drained)
					return;
				if (quads() > 0 && !emptySubmitted)
				{
					renderSample(0);
					emptySubmitted = true;
					return;
				}
				if (emptySubmitted)
				{
					GpuTiming.Result empty = results.removeLast();
					check(empty.fullscreen() == 0 && empty.allocations() == 0 &&
							!empty.milliseconds().containsKey("depth") && !empty.milliseconds().containsKey("metadata"),
							"Empty frame following OIT must skip all OIT passes and allocations");
					PLibDatabase.LOGGER.info("H14 GPU EMPTY FRAME PASS: retained resources, no OIT draws");
				}
				writeRow();
				closeScene();
				configuration++;
				if (configuration == 24)
				{
					Path output = PerfRun.output();
					Files.createDirectories(output);
					Files.write(output.resolve("gpu-performance.csv"), rows);
					PLibDatabase.LOGGER.info("H14 GPU PERFORMANCE PASS: 24 scenarios, 32 warmup + 80 samples each; {}", output);
					completed = true;
					PerfRun.complete("offscreen", 24, WARMUP, SAMPLES);
					mc.stop();
				}
				return;
			}
			renderSample(quads());
			frame++;
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("Cannot profile OIT", exception);
		}
	}

	private static void configure() throws IOException
	{
		check(GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_timer_query,
				"GPU timestamps require OpenGL 3.3 or ARB_timer_query");
		Minecraft mc = Minecraft.getInstance();
		if (configuration == 0)
			PLibDatabase.LOGGER.info("H14 GPU DEVICE: {} / {} / {}", GL11.glGetString(GL11.GL_VENDOR),
					GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION));
		int[][] dimensions = {{1920, 1080}, {2560, 1440}, {3840, 2160}};
		int[] size = dimensions[configuration / 8];
		initialAvailableVram = availableVram();
		check(size[0] <= GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE), "Requested offscreen resolution is unsupported");
		main = new TextureTarget("GPU performance", size[0], size[1], true);
		identity = new ProjectionMatrixBuffer("GPU profile identity");
		if (fabulous())
			chain = new PerfTargets(size[0], size[1]);
		geometry = H14OffscreenHarness.atlasQuad();
		frame = 0;
		emptySubmitted = false;
		results.clear();
		warmResourceIds = null;
		sampleDeadline = System.nanoTime() + 180_000_000_000L;
		if (rows.isEmpty())
			rows.add("mode,width,height,quads,samples,frame_mean_ms,frame_p95_ms,depth_ms,accumulation_ms,metadata_ms,compositor_ms,vanilla_ms,oit_texture_mib,metadata_mib,vanilla_texture_mib,oit_fullscreen_draws,vanilla_framegraph_passes,steady_texture_allocations,buffer_data_calls,driver_vram_delta_mib");
	}

	static RenderTarget main()
	{
		return main;
	}

	private static boolean fabulous()
	{
		return configuration % 8 >= 4;
	}

	private static int quads()
	{
		return new int[]{0, 1, 16, 128}[configuration % 4];
	}

	private static void renderSample(int quadCount)
	{
		Minecraft mc = Minecraft.getInstance();
		var projection = RenderSystem.getProjectionMatrixBuffer();
		ProjectionType sorting = RenderSystem.getProjectionType();
		RenderSystem.getModelViewStack().pushMatrix();
		try (var state = new GlRenderState())
		{
			RenderSystem.getModelViewStack().identity();
			RenderSystem.setProjectionMatrix(identity.getBuffer(new Matrix4f()), ProjectionType.ORTHOGRAPHIC);
			RenderSystem.outputColorTextureOverride = main.getColorTextureView();
			RenderSystem.outputDepthTextureOverride = main.getDepthTextureView();
			if (frame == WARMUP && quadCount > 0)
				warmResourceIds = resourceIds();
			GpuTiming.start();
			PerfTargets.clear(main, 0xFF333333);
			if (chain != null)
				chain.clear();
			var type = PRenderTypes.RenderTypeProvider.trianglesEmissiveTranslucent(WHITE);
			for (int index = 0; index < quadCount; index++)
				PRenderQueue.submit(PRenderQueue.RenderStage.ENTITIES, type, geometry, new PInstanceHeader(
						new Matrix4f().translation(0f, 0f, -0.6f + (index % 4) * 0.1f),
						0x4080C0FF, 0xF000F0, OverlayTexture.NO_OVERLAY), PTransparencyMode.OIT);
			PRenderQueue.flush(PRenderQueue.RenderStage.ENTITIES, main);
			if (chain != null)
			{
				GpuTiming.begin("vanilla");
				chain.process();
				GpuTiming.end();
				List<RenderTarget> targets = chain.targets;
				if (!Boolean.getBoolean("pulselib.test.perfSkipComposite"))
					PRenderQueue.compositeFabulousTranslucency(main, targets);
			}
			else
				if (!Boolean.getBoolean("pulselib.test.perfSkipComposite"))
					PRenderQueue.compositeTranslucency(main);
			GpuTiming.finish(frame >= WARMUP);
			GL11.glFlush();
		}
		finally
		{
			RenderSystem.getModelViewStack().popMatrix();
			RenderSystem.setProjectionMatrix(projection, sorting);
		}
	}

	private static void writeRow()
	{
		check(results.size() == SAMPLES, "All GPU samples must retire");
		check(warmResourceIds == null || warmResourceIds.equals(resourceIds()), "GPU resources must remain stable after warmup");
		int expectedDraws = quads() == 0 ? 0 : fabulous() ? 8 : 4;
		check(results.stream().allMatch(result -> result.fullscreen() == expectedDraws), "Unexpected OIT fullscreen draw count");
		check(results.stream().allMatch(result -> result.allocations() == 0), "Steady-state OIT textures must be reused");
		long oitBytes = PerfResources.bytes(false);
		long metadataBytes = PerfResources.bytes(true);
		long vanillaBytes = chain == null ? 0 : chain.bytes();
		double[] frames = results.stream().mapToDouble(result -> result.milliseconds().get("frame")).sorted().toArray();
		String row = String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.3f,%.3f,%.3f,%d,%d,0,%.3f,%.3f",
				fabulous() ? "Fabulous" : "Fancy", main.width, main.height, quads(), SAMPLES,
				mean("frame"), frames[(int)Math.ceil(SAMPLES * 0.95) - 1], mean("depth"), mean("accumulation"),
				mean("metadata"), mean("compositor"), mean("vanilla"),
				(oitBytes + metadataBytes) / 1048576.0, metadataBytes / 1048576.0, vanillaBytes / 1048576.0,
				expectedDraws, chain == null ? 0 : chain.passes,
				results.stream().mapToInt(GpuTiming.Result::bufferReallocations).average().orElseThrow(),
				initialAvailableVram < 0 ? -1.0 : (initialAvailableVram - availableVram()) / 1024.0);
		rows.add(row);
		PLibDatabase.LOGGER.info("H14 GPU ROW {}", row);
		check(GL11.glGetError() == GL11.GL_NO_ERROR, "OIT profile produced an OpenGL error");
	}

	private static double mean(String name)
	{
		return results.stream().mapToDouble(result -> result.milliseconds().getOrDefault(name, 0.0)).average().orElseThrow();
	}

	static long textureBytes(int texture)
	{
		if (texture <= 0)
			return 0L;
		int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		try
		{
			int bits = 0;
			for (int channel : new int[]{GL11.GL_TEXTURE_RED_SIZE, GL11.GL_TEXTURE_GREEN_SIZE,
					GL11.GL_TEXTURE_BLUE_SIZE, GL11.GL_TEXTURE_ALPHA_SIZE, GL14.GL_TEXTURE_DEPTH_SIZE})
				bits += GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, channel);
			return (long)GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH) *
					GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT) * bits / 8L;
		}
		finally
		{
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
		}
	}

	static long availableVram()
	{
		return GL.getCapabilities().GL_NVX_gpu_memory_info ?
				GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX) : -1L;
	}

	static List<Integer> resourceIds()
	{
		return PerfResources.ids();
	}

	private static void closeScene()
	{
		PRenderQueue.cleanup();
		if (chain != null)
		{
			chain.close();
			chain = null;
		}
		identity.close();
		identity = null;
		main.destroyBuffers();
		main = null;
	}
}
