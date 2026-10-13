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
import com.arcanc.pulselib.content.renderer.legacy.GlStreamStorage;
import com.arcanc.pulselib.content.renderer.plan.PGeometryData;
import com.arcanc.pulselib.content.renderer.plan.PInstanceHeader;
import com.arcanc.pulselib.util.PLibDatabase;
import com.arcanc.pulselib.util.PModelCache;
import com.arcanc.pulselib.util.PRenderTypes;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
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

import static com.arcanc.pulselib.test.SmokeHarness.check;
import static com.arcanc.pulselib.test.SmokeHarness.field;

/**
 * Warm, persistent offscreen fixtures at real render resolutions. No glFinish,
 * pixel readback or CPU timer occurs in measured frames. This measures the
 * fixture's GPU frame, not terrain/GUI/presentation in a normal Minecraft frame.
 */
public final class OitPerformanceHarness
{
	private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("pulselibtest", "perf_white");
	private static final String[] TARGETS = {"translucent", "itemEntity", "particles", "weather", "clouds"};
	private static final int WARMUP = 32;
	private static final int SAMPLES = 80;
	private static final List<GpuTiming.Result> results = new ArrayList<>();
	private static final List<String> rows = new ArrayList<>();
	private static @Nullable TextureTarget main;
	private static @Nullable PostChain chain;
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
		{
			return;
		}
		check(mc.level == null, "Offscreen profiling cannot run while a world is loaded");
		try
		{
			if (main == null)
			{
				configure();
			}
			boolean drained = GpuTiming.drain(results);
			if (frame >= WARMUP + SAMPLES)
			{
				check(System.nanoTime() < sampleDeadline, "GPU timer results did not become available");
				if (!drained)
				{
					return;
				}
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
					Path output = Path.of(System.getProperty("pulselib.test.oitOutput", "h14-results"));
					Files.createDirectories(output);
					Files.write(output.resolve("gpu-performance.csv"), rows);
					PLibDatabase.LOGGER.info("H14 GPU PERFORMANCE PASS: 24 scenarios, 32 warmup + 80 samples each; {}", output);
					completed = true;
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
		{
			PLibDatabase.LOGGER.info("H14 GPU DEVICE: {} / {} / {}", GL11.glGetString(GL11.GL_VENDOR),
					GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION));
		}
		int[][] dimensions = {{1920, 1080}, {2560, 1440}, {3840, 2160}};
		int[] size = dimensions[configuration / 8];
		initialAvailableVram = availableVram();
		check(size[0] <= GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE), "Requested offscreen resolution is unsupported");
		main = new TextureTarget(size[0], size[1], true, Minecraft.ON_OSX);
		if (fabulous())
		{
			chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main,
					ResourceLocation.withDefaultNamespace("shaders/post/transparency.json"));
			chain.resize(size[0], size[1]);
		}
		NativeImage white = new NativeImage(1, 1, false);
		white.setPixelRGBA(0, 0, -1);
		mc.getTextureManager().register(WHITE, new DynamicTexture(white));
		geometry = H14Harness.quad();
		frame = 0;
		emptySubmitted = false;
		results.clear();
		warmResourceIds = null;
		sampleDeadline = System.nanoTime() + 180_000_000_000L;
		if (rows.isEmpty())
		{
			rows.add("mode,width,height,quads,samples,frame_mean_ms,frame_p95_ms,depth_ms,accumulation_ms,metadata_ms,compositor_ms,vanilla_ms,oit_texture_mib,metadata_mib,vanilla_texture_mib,oit_fullscreen_draws,vanilla_fullscreen_draws,steady_texture_allocations,buffer_data_calls,driver_vram_delta_mib");
		}
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
		Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting sorting = RenderSystem.getVertexSorting();
		RenderSystem.getModelViewStack().pushMatrix();
		try
		{
			RenderSystem.getModelViewStack().identity();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
			if (frame == WARMUP && quadCount > 0)
			{
				warmResourceIds = resourceIds();
			}
			GpuTiming.start();
			H14Harness.clear(main, 0.2f, 0.2f, 0.2f, 1f, 1f);
			if (chain != null)
			{
				for (String target : TARGETS)
				{
					H14Harness.clear(chain.getTempTarget(target), 0f, 0f, 0f, 0f, 1f);
				}
				main.bindWrite(true);
			}
			var type = PRenderTypes.RenderTypeProvider.trianglesTranslucentEmissive(WHITE);
			for (int index = 0; index < quadCount; index++)
			{
				PRenderQueue.submitEntityMesh(type, geometry, new PInstanceHeader(
						new Matrix4f().translation(0f, 0f, -0.6f + (index % 4) * 0.1f),
						0x4080C0FF, 0xF000F0, OverlayTexture.NO_OVERLAY));
			}
			PRenderQueue.flush(PRenderQueue.RenderStage.ENTITIES, main);
			if (chain != null)
			{
				GpuTiming.begin("vanilla");
				chain.process(0f);
				GpuTiming.end();
				List<RenderTarget> targets = Arrays.stream(TARGETS).map(chain::getTempTarget).toList();
				PRenderQueue.compositeFabulousTranslucency(main, targets);
			}
			else
			{
				PRenderQueue.compositeTranslucency(main);
			}
			GpuTiming.finish(frame >= WARMUP);
			GL11.glFlush();
		}
		finally
		{
			RenderSystem.getModelViewStack().popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(projection, sorting);
			mc.getMainRenderTarget().bindWrite(true);
		}
	}

	private static void writeRow()
	{
		check(results.size() == SAMPLES, "All GPU samples must retire");
		check(warmResourceIds == null || warmResourceIds.equals(resourceIds()), "GPU resources must remain stable after warmup");
		int expectedDraws = quads() == 0 ? 0 : fabulous() ? 8 : 4;
		check(results.stream().allMatch(result -> result.fullscreen() == expectedDraws), "Unexpected OIT fullscreen draw count");
		check(results.stream().allMatch(result -> result.allocations() == 0), "Steady-state OIT textures must be reused");
		Object executor = field(PRenderQueue.class, "EXECUTOR", null);
		Object oit = field(executor.getClass(), "weightedBlendedOit", executor);
		long oitBytes = 0L;
		for (String name : List.of("accumulationTextures", "revealageTextures", "layerDepthTextures"))
		{
			for (int texture : (int[])field(oit.getClass(), name, oit))
			{
				oitBytes += textureBytes(texture);
			}
		}
		long metadataBytes = textureBytes((Integer)field(oit.getClass(), "fabulousMetadataTexture", oit));
		long vanillaBytes = 0L;
		if (chain != null)
		{
			for (String name : List.of("water", "translucent", "itemEntity", "particles", "weather", "clouds", "final"))
			{
				RenderTarget target = chain.getTempTarget(name);
				vanillaBytes += textureBytes(target.getColorTextureId()) + textureBytes(target.getDepthTextureId());
			}
		}
		double[] frames = results.stream().mapToDouble(result -> result.milliseconds().get("frame")).sorted().toArray();
		String row = String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.3f,%.3f,%.3f,%d,%d,0,%.3f,%.3f",
				fabulous() ? "Fabulous" : "Fancy", main.width, main.height, quads(), SAMPLES,
				mean("frame"), frames[(int)Math.ceil(SAMPLES * 0.95) - 1], mean("depth"), mean("accumulation"),
				mean("metadata"), mean("compositor"), mean("vanilla"),
				(oitBytes + metadataBytes) / 1048576.0, metadataBytes / 1048576.0, vanillaBytes / 1048576.0,
				expectedDraws, fabulous() ? 2 : 0,
				results.stream().mapToInt(GpuTiming.Result::bufferReallocations).average().orElseThrow(),
				initialAvailableVram < 0 ? -1.0 : (initialAvailableVram - availableVram()) / 1024.0);
		rows.add(row);
		PLibDatabase.LOGGER.info("H14 GPU ROW {}", row);
		PLibDatabase.LOGGER.info("H14 GPU BACKEND: persistent instances {}, persistent indirect {}, multi draw indirect {}",
				field(executor.getClass(), "persistentMapping", executor), field(executor.getClass(), "persistentIndirectMapping", executor),
				field(executor.getClass(), "multiDrawIndirect", executor));
		check(GL11.glGetError() == GL11.GL_NO_ERROR, "OIT profile produced an OpenGL error");
	}

	private static double mean(String name)
	{
		return results.stream().mapToDouble(result -> result.milliseconds().getOrDefault(name, 0.0)).average().orElseThrow();
	}

	static long textureBytes(int texture)
	{
		if (texture <= 0)
		{
			return 0L;
		}
		int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		try
		{
			int bits = 0;
			for (int channel : new int[]{GL11.GL_TEXTURE_RED_SIZE, GL11.GL_TEXTURE_GREEN_SIZE,
					GL11.GL_TEXTURE_BLUE_SIZE, GL11.GL_TEXTURE_ALPHA_SIZE, GL14.GL_TEXTURE_DEPTH_SIZE})
			{
				bits += GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, channel);
			}
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
		List<Integer> ids = new ArrayList<>();
		Object executor = field(PRenderQueue.class, "EXECUTOR", null);
		Object oit = field(executor.getClass(), "weightedBlendedOit", executor);
		for (String name : List.of("accumulationTextures", "revealageTextures", "layerDepthTextures"))
		{
			for (int id : (int[])field(oit.getClass(), name, oit))
			{
				ids.add(id);
			}
		}
		ids.add((Integer)field(oit.getClass(), "fabulousMetadataTexture", oit));
		ids.add((Integer)field(oit.getClass(), "framebuffer", oit));
		Object instance = field(executor.getClass(), "instanceBuffer", executor);
		Object indirect = field(executor.getClass(), "indirectBuffer", executor);
		ids.add((Integer)field(instance.getClass(), "vbo", instance));
		ids.add((Integer)field(instance.getClass(), "capacity", instance));
		ids.add((Integer)field(indirect.getClass(), "buffer", indirect));
		ids.add((Integer)field(indirect.getClass(), "capacity", indirect));
		Object registry = field(PRenderQueue.class, "RESOURCES", null);
		Object arena = field(registry.getClass(), "geometryArena", registry);
		for (Object page : (List<?>)field(arena.getClass(), "pages", arena))
		{
			ids.add((Integer)field(page.getClass(), "vertexBuffer", page));
			ids.add((Integer)field(page.getClass(), "indexBuffer", page));
		}
		for (String name : List.of("operationsBuffer", "valuesBuffer", "operationsTexture", "valuesTexture"))
		{
			ids.add((Integer)field(GlStreamStorage.class, name, null));
		}
		return List.copyOf(ids);
	}

	private static void closeScene()
	{
		PRenderQueue.cleanup();
		if (chain != null)
		{
			chain.close();
			chain = null;
		}
		main.destroyBuffers();
		main = null;
	}
}
